package enl.sif.path;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKEINTERFACE;
import org.apache.bcel.generic.INVOKESPECIAL;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReferenceType;

import android.os.Handler;
import enl.sif.Config;
import enl.sif.codepoint.ClassFileScanner;
import enl.sif.codepoint.MethodMap;
import enl.sif.codepoint.Util;

public class CallHierarchyBuilder {

	// collect call site information for building a call graph for user-defined methods
	public static Set<CallSite> build(String dir) {
		Set<CallSite> cs_set = new HashSet<CallSite>();
		Map<String, ClassGen> map = new HashMap<String, ClassGen>();

		// 0. scan to get mapping: ClassName --> ClassGen
		for (String class_fn : ClassFileScanner.getAllClassFileNames(dir)) {
			JavaClass jcls = null;

			try {
				jcls = new ClassParser(class_fn).parse();
			} catch (IOException e) {
				e.printStackTrace();
			}

			assert jcls != null : "JavaClass is NULL";

			ClassGen cgen = new ClassGen(jcls);
			String cname = jcls.getClassName();
			map.put(cname, cgen);

			List<String> super_cnames = new ArrayList<String>();

			try {
				JavaClass[] sup_jclss = jcls.getSuperClasses();
				for (JavaClass sup_jcls : sup_jclss) {
					String sup_cname = sup_jcls.getClassName();

					// class hierarchy will not include the class itself
					super_cnames.add(sup_cname);
				}

				JavaClass[] iface_jclss = jcls.getAllInterfaces();
				for (JavaClass iface_jcls : iface_jclss) {
					String iface_cname = iface_jcls.getClassName();

					if (!cname.equals(iface_cname)) { // interface hierarchy will include the class itself
						super_cnames.add(iface_cname);
					}
				}

				ClassHierarchyMap.add(cname, super_cnames);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		Util.log("#classes=" + map.size());

		ClassHierarchyMap.buildMap();

		// 1. go through each class and build mapping: CallSite --> List of potential methods
		for (Iterator<Entry<String, ClassGen>> iter = map.entrySet().iterator(); iter.hasNext();) {
			Entry<String, ClassGen> entry = iter.next();
			String cname = entry.getKey();
			ClassGen cgen = entry.getValue();

			if (cgen.isInterface()) { // all interfaces can only have abstract method (i.e. w/o instructions) 
				continue;
			}

			ConstantPoolGen cpgen = cgen.getConstantPool();

			// 1.1 go through each method
			for (Method mthd : cgen.getMethods()) {
				String mname = mthd.getName();
				String msig = mthd.getSignature();
				MethodGen mgen = new MethodGen(mthd, cname, cpgen);
				InstructionList ilist = mgen.getInstructionList();

				if (ilist == null) { // e.g. abstract/native method have no body
					Util.log("Method has no body");
					continue;
				}

				if (mname.equals("<clinit>")) {
					Util.log("We don't handle <clinit>");
					continue;
				}

				InstructionHandle[] ihdls = ilist.getInstructionHandles();
				int caller_mid = MethodMap.getMethodID(cname, mname + ":" + msig);

				assert (caller_mid >= 0) : "ERR: " + cname + "." + mname + msig + " does not exist";

				Util.log(cname + "." + mthd.getName() + " | " + caller_mid);

				// 1.2 go through each instruction
				for (InstructionHandle ihdl : ihdls) {
					Instruction instr = ihdl.getInstruction();

					// 1.2.1 only check invokes 
					if (instr instanceof InvokeInstruction) {
						InvokeInstruction invoke = (InvokeInstruction) instr;
						ReferenceType obj_ref = invoke.getReferenceType(cpgen);

						String invoke_cname = obj_ref.toString();
						String invoke_mname = invoke.getMethodName(cpgen);
						String invoke_msig = invoke.getSignature(cpgen);

						Util.log("\tcalling " + invoke_cname + "." + invoke_mname + invoke_msig + " | " + invoke.getName());

						Set<String> taint_cnames = new HashSet<String>();

						// 1.2.2 handle case by case
						if (invoke instanceof INVOKEINTERFACE) {
							taint_cnames = handle_invokeinterface(invoke_cname, invoke_mname, invoke_msig, map);
						} else if (invoke instanceof INVOKEVIRTUAL) {
							taint_cnames = handle_invokevirtual(invoke_cname, invoke_mname, invoke_msig, map);
						} else if (invoke instanceof INVOKESPECIAL) {
							taint_cnames = handle_invokespecial(invoke_cname, invoke_mname, invoke_msig, map);
						} else if (invoke instanceof INVOKESTATIC) {
							taint_cnames = handle_invokestatic(invoke_cname, invoke_mname, invoke_msig, map);
						} else {
							// UNREACHABLE
							Util.err("Unknown invoke instruction");
						}

						// IMPORTANT: Runnable.post*(Runnable) == SubClass.run()
						if (invoke_cname.equals(Handler.class.getCanonicalName()) && invoke_mname.startsWith("post")) {
							taint_cnames.addAll(handle_runnable(Runnable.class.getCanonicalName(), map));
						}

						if (taint_cnames.isEmpty()) { // this must be an API invoke
							Util.log("\t\tAPI");
							continue;
						}

						Util.log("\t\t|callees|=" + taint_cnames.size() + " " + taint_cnames.toString());

						// 1.2.3 convert methods to mids
						Set<Integer> callee_mids = new HashSet<Integer>();
						String tmp;
						for (Iterator<String> _iter = taint_cnames.iterator(); _iter.hasNext();) {
							String callee_cname = _iter.next();
							tmp = null;

							// IMPORTANT: Runnable.post*(Runnable) == SubClass.run()
							if (check_thread(callee_cname) && invoke_mname.startsWith("post")) {
								tmp = "run";
							}

							// IMPORTANT: Thread.start() == SubClass.run()							
							if (check_thread(callee_cname) && invoke_mname.equals("start") && invoke_msig.equals("()V")) {
								tmp = "run";
							}

							int callee_mid;

							if (tmp != null) {
								callee_mid = MethodMap.getMethodID(callee_cname, "run:()V");
								assert (callee_mid >= 0) : "ERR: " + callee_cname + ".run()V does not exist";
							} else {
								callee_mid = MethodMap.getMethodID(callee_cname, invoke_mname + ":" + invoke_msig);
								assert (callee_mid >= 0) : "ERR: " + callee_cname + "." + invoke_mname + invoke_msig + " does not exist";
							}
							callee_mids.add(callee_mid);
						}

						// 1.2.4 link call site to set of callees
						int pos = ihdl.getPosition();
						CallSite call_site = new CallSite(caller_mid, pos, callee_mids);
						cs_set.add(call_site);

					} // if InvokeInstruction
				} // for InstructionHandle
			} // for Method
		} // for Class

		// 2. now we have all the call site info

		// debug only
		int tot = 0;
		for (Iterator<CallSite> iter = cs_set.iterator(); iter.hasNext();) {
			tot += iter.next().callees.size();
		}
		Util.log("|V|=" + cs_set.size() + ", |E|=" + tot);

		return cs_set;
	}

	private static boolean check_thread(String cname) {
		JavaClass callee_jcls = Util.loadJavaClass(Config.INPUT_DIR + "/" + cname.replace('.', '/') + ".class");
		try {
			JavaClass[] callee_sup_jclss = callee_jcls.getSuperClasses();
			for (JavaClass callee_sup_jcls : callee_sup_jclss) {
				if (callee_sup_jcls.getClassName().equals(Thread.class.getCanonicalName()))
					return true;
			}

			callee_sup_jclss = callee_jcls.getAllInterfaces();
			for (JavaClass callee_sup_jcls : callee_sup_jclss) {
				if (callee_sup_jcls.getClassName().equals(Runnable.class.getCanonicalName()))
					return true;
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		return false;
	}

	private static Set<String> handle_runnable(String invoke_cname, Map<String, ClassGen> map) {
		Set<String> ret = new HashSet<String>();

		// only need to check sub classes
		Set<String> sub_cnames = ClassHierarchyMap.getSubClassNames(invoke_cname);
		if (!sub_cnames.isEmpty()) {
			for (Iterator<String> _iter = sub_cnames.iterator(); _iter.hasNext();) {
				String sub_cname = _iter.next();

				assert (map.containsKey(sub_cname)) : "ERR: " + sub_cname + " not in map";
				ret.add(sub_cname);
			}
		}

		return ret;
	}

	private static Set<String> handle_invokeinterface(String invoke_cname, String invoke_mname, String invoke_msig, Map<String, ClassGen> map) {
		Set<String> ret = new HashSet<String>();

		// only need to check sub classes
		Set<String> sub_cnames = ClassHierarchyMap.getSubClassNames(invoke_cname);
		if (!sub_cnames.isEmpty()) {
			for (Iterator<String> _iter = sub_cnames.iterator(); _iter.hasNext();) {
				String sub_cname = _iter.next();

				assert (map.containsKey(sub_cname)) : "ERR: " + sub_cname + " not in map";

				ClassGen sub_cgen = map.get(sub_cname);
				Method sub_mthd = sub_cgen.containsMethod(invoke_mname, invoke_msig);

				if (sub_mthd != null && !sub_mthd.isAbstract() && (sub_mthd.isPublic() || sub_mthd.isProtected())) {
					ret.add(sub_cname);
				}
			}
		}

		return ret;
	}

	private static Set<String> handle_invokevirtual(String invoke_cname, String invoke_mname, String invoke_msig, Map<String, ClassGen> map) {
		Set<String> ret = new HashSet<String>();

		// 0. check sub classes first
		Set<String> sub_cnames = ClassHierarchyMap.getSubClassNames(invoke_cname);
		if (!sub_cnames.isEmpty()) {
			for (Iterator<String> iter = sub_cnames.iterator(); iter.hasNext();) {
				String sub_cname = iter.next();

				assert (map.containsKey(sub_cname)) : "ERR: " + sub_cname + " not in map";

				ClassGen sub_cgen = map.get(sub_cname);
				Method sub_mthd;

				if (invoke_cname.equals("java.lang.Thread") && invoke_mname.equals("start")) {
					sub_mthd = sub_cgen.containsMethod("run", invoke_msig);
				} else {
					sub_mthd = sub_cgen.containsMethod(invoke_mname, invoke_msig);
				}

				if (sub_mthd != null && !sub_mthd.isAbstract() && (sub_mthd.isPublic() || sub_mthd.isProtected())) {
					ret.add(sub_cname);
				}
			}
		}

		if (map.containsKey(invoke_cname)) {
			ClassGen invoke_cgen = map.get(invoke_cname);

			// 1. check my own methods
			Method own_mthd = invoke_cgen.containsMethod(invoke_mname, invoke_msig);
			if (own_mthd != null && !own_mthd.isAbstract()) { // 1.1 stop, found it in the defining class
				ret.add(invoke_cname);
			} else { // 2. check my super classes
				try {
					JavaClass[] sup_jclss = invoke_cgen.getJavaClass().getSuperClasses();
					for (JavaClass sup_jcls : sup_jclss) {
						String sup_cname = sup_jcls.getClassName();

						if (!map.containsKey(sup_cname)) {
							continue;
						}

						ClassGen sup_cgen = map.get(sup_cname); // new ClassGen(sup_jcls);
						Method sup_mthd = sup_cgen.containsMethod(invoke_mname, invoke_msig);

						// 2.1 stop when we find the first super class that has implemented our target method 
						if (sup_mthd != null && !sup_mthd.isAbstract() && (sup_mthd.isPublic() || sup_mthd.isProtected())) {
							ret.add(sup_cgen.getClassName());
							break;
						}
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		return ret;
	}

	// INVOKESPECIAL is used for constructor, this.private_method(), super.method()
	private static Set<String> handle_invokespecial(String invoke_cname, String invoke_mname, String invoke_msig, Map<String, ClassGen> map) {
		Set<String> ret = new HashSet<String>();

		if (!map.containsKey(invoke_cname)) { // 0. we will lose control anyway
			return ret;
		}

		ClassGen invoke_cgen = map.get(invoke_cname);

		if (invoke_mname.equals("<init>")) { // 1. constructor, no doubt
			ret.add(invoke_cname);
		} else {
			Method own_mthd = invoke_cgen.containsMethod(invoke_mname, invoke_msig);

			if (own_mthd != null && !own_mthd.isAbstract()) { // 2. must be invoking this.private_method() or super.method() and super has implemented such method
				ret.add(invoke_cname);
			} else { // 3. must be invoking super.method() and super does not implement such method
				try {
					JavaClass[] sup_jclss = invoke_cgen.getJavaClass().getSuperClasses();

					for (JavaClass sup_jcls : sup_jclss) {
						String sup_cname = sup_jcls.getClassName();

						Util.log(sup_cname);
						if (!map.containsKey(sup_cname)) {
							continue;
						}

						ClassGen sup_cgen = map.get(sup_cname); // new ClassGen(sup_jcls);
						Method sup_mthd = sup_cgen.containsMethod(invoke_mname, invoke_msig);

						if (sup_mthd != null && !sup_mthd.isAbstract() && (sup_mthd.isPublic() || sup_mthd.isProtected())) {
							ret.add(sup_cgen.getClassName());
							break;
						}
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		// assert (!ret.isEmpty()) : "ERR: Couldn't find any callee";

		return ret;
	}

	// callee is resolved in compile-time, but it's still possible to be in super class
	private static Set<String> handle_invokestatic(String invoke_cname, String invoke_mname, String invoke_msig, Map<String, ClassGen> map) {
		Set<String> ret = new HashSet<String>();

		if (!map.containsKey(invoke_cname)) {
			return ret;
		}

		ClassGen invoke_cgen = map.get(invoke_cname);
		Method own_mthd = invoke_cgen.containsMethod(invoke_mname, invoke_msig);

		if (own_mthd != null && !own_mthd.isAbstract()) {
			ret.add(invoke_cname);
		} else {
			try {
				JavaClass[] sup_jclss = invoke_cgen.getJavaClass().getSuperClasses();
				for (JavaClass sup_jcls : sup_jclss) {
					String sup_cname = sup_jcls.getClassName();

					if (!map.containsKey(sup_cname)) {
						continue;
					}

					ClassGen sup_cgen = map.get(sup_cname);
					Method sup_mthd = sup_cgen.containsMethod(invoke_mname, invoke_msig);

					if (sup_mthd != null && !sup_mthd.isAbstract() && (sup_mthd.isPublic() || sup_mthd.isProtected())) {
						ret.add(sup_cgen.getClassName());
						break;
					}
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}

		return ret;
	}
}
