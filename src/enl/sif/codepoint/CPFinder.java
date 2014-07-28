package enl.sif.codepoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import enl.sif.Config;

public class CPFinder {

	private static ClassFilter cf;
	private static MethodFilter mf;
	private static BytecodeFilter bf;
	private static PermissionFilter pf;

	public static void init() {
		cf = new ClassFilter();
		mf = new MethodFilter();
		bf = new BytecodeFilter();
		pf = new PermissionFilter();
	}

	public static void setPermission(AndroidVersion version, String... perm) {
		PermissionMap.load(version);
		pf.filters = perm;
	}

	public static void setBytecode(BytecodePosition pos) {
		bf.pos = pos;
	}

	public static void setBytecode(short type) {
		bf.type = type;
	}

	public static void setBytecode(short type, String filter) {
		bf.type = type;
		bf.filter = filter;
	}

	public static void setMethod(String filter) {
		mf.nameFilter = filter;
	}

	public static void setClass(String name_filter, String hierarchy_filter) {
		cf.nameFilter = name_filter;
		cf.hierachyFilter = hierarchy_filter;
	}

	public static Set<CP> apply() {
		Set<CP> ret = new HashSet<CP>();

		// debug
		Util.log("In apply()");

		Util.log((cf == null || !cf.isSet()) ? "ClassFilter: unset" : cf.toString());
		Util.log((mf == null || !mf.isSet()) ? "MethodFilter: unset" : mf.toString());
		Util.log((bf == null || !bf.isSet()) ? "BytecodeFilter: unset" : bf.toString());
		Util.log((pf == null || !pf.isSet()) ? "PermissionFilter: unset" : pf.toString());

		String dir = Config.INPUT_DIR;
		MethodMap.load(dir + "/../sifa/method.map");

		// 0. go through individual class
		for (String class_fn : get_all_class_fn(dir)) {
			JavaClass jcls = null;

			try {
				jcls = new ClassParser(class_fn).parse();
			} catch (ClassFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			assert jcls != null : "JavaClass is NULL";
			ClassGen cgen = new ClassGen(jcls);
			ConstantPoolGen cpgen = cgen.getConstantPool();

			// 1. apply class filter
			String cls_name = cgen.getClassName();

			if (!apply_classfilter(jcls)) {
				continue;
			}

			// Util.log("Found Class: " + cls_name);
			// cls_names.add(cls_name);

			// 2. apply method filter
			for (Method mthd : jcls.getMethods()) {
				boolean matched = false;
				String mthd_sig = "";

				MethodGen method = new MethodGen(mthd, cgen.getClassName(), cgen.getConstantPool());
				if (!method.isAbstract() && !method.isInterface() && !method.getName().equals("<clinit>")) {
					// only scan methods that are can be executed in real life

					// mthd_sig = method.toString(); // used by Timing profiling
					mthd_sig = method.getName() + ":" + method.getSignature();
					// Util.log(mthd_sig + " <---> " + mf.nameFilter + " " + mthd_sig.matches(mf.nameFilter));

					if (mf.nameFilter == null) {
						matched = true;
					} else {
						matched = mthd_sig.matches(mf.nameFilter);
					}

					// mthd_sig = method.getName() + ":" + method.getSignature();
				}

				if (matched) {
					// Util.log("\tFound Method: " + method);
				} else {
					continue;
				}

				int mid = MethodMap.getMethodID(cls_name, mthd_sig);
				assert (mid != -1) : "mid=-1 for " + cls_name + "," + mthd_sig;

				// 3. apply bytecode filter
				InstructionList ilist = method.getInstructionList();
				if (ilist == null || ilist.size() == 0) {
					continue;
				}

				InstructionHandle[] ihdls = ilist.getInstructionHandles();
				BytecodePosition pos = bf.pos;
				matched = false;

				if (!bf.isSet()) {
					// scan to check permission filter
					for (InstructionHandle ihdl : ihdls) {
						Instruction instr = ihdl.getInstruction();

						if (apply_permfilter(instr, pf, cpgen)) {
							Util.log("\t\tFound Bytecode 0: " + instr);
							ret.add(new CP(mid, ihdl.getPosition()));
						}
					}
					continue;
				}

				if (pos != null) { // check BytecodePosition filter, at most ONE match
					if (pos.isSpecialPosition()) {
						// TODO: other special position may include LOOP-related positions
						matched = true;
					} else { // we still need to scan for matches
						for (InstructionHandle ihdl : ihdls) {
							if (pos.getValue() == ihdl.getPosition()) {
								if (pf.isSet()) {
									matched = apply_permfilter(ihdl.getInstruction(), pf, cpgen);
								} else {
									matched = true;
								}
								break;
							}
						}
					}

					if (matched) {
						Util.log("\t\tFound Bytecode 1: " + pos);
						ret.add(new CP(mid, pos.getValue()));
					}
				} else { // check type and name filter, may have MULTIPLE matches
					for (InstructionHandle ihdl : ihdls) {
						Instruction instr = ihdl.getInstruction();
						int type = instr.getOpcode();

						if (type != bf.type)
							continue;

						if (bf.filter != null) {
							// TODO: currently filter is only for invoke instructions
							if (instr instanceof InvokeInstruction) {
								String isig = get_invoke_sig((InvokeInstruction) instr, cpgen);
								// Util.log(isig + " <--> " + bf.filter + " " + isig.matches(bf.filter));
								matched = isig.matches(bf.filter);
								if (matched) {
									Util.log(isig);
								}
							}
						} else {
							matched = true;
						}

						if (pf.isSet()) {
							matched &= apply_permfilter(ihdl.getInstruction(), pf, cpgen);
						}

						if (matched) {
							Util.log("\t\tFound Bytecode 2: " + instr.toString(cpgen.getConstantPool()));
							ret.add(new CP(mid, ihdl.getPosition()));
						}
					} // end for ihdl
				} // end else pos
			} // end for method
		} // end for class

		Util.log(ret.toString());
		return ret;
	}

	private static String get_invoke_sig(InvokeInstruction invoke, ConstantPoolGen cpgen) {
		ReferenceType ctype = invoke.getReferenceType(cpgen);
		String cname = ctype.toString();
		String mname = invoke.getName(cpgen);
		Type[] arg_t = invoke.getArgumentTypes(cpgen);

		if ((ctype instanceof ObjectType) && !(cname.startsWith("com.google") || cname.startsWith("java") || cname.startsWith("org.") || cname.startsWith("android."))) { // we only care about user-defined class
			JavaClass jcls = Util.loadJavaClass(Config.INPUT_DIR + cname.replace('.', '/') + ".class");
			ClassGen cgen = new ClassGen(jcls);

			String msig = "(";
			for (Type t : arg_t) {
				msig += t.getSignature();
			}
			msig += ")" + invoke.getReturnType(cpgen).getSignature();
			// Util.log(cname + "." + mname + ":" + msig);

			Method mthd = cgen.containsMethod(mname, msig);
			//			if (mthd == null) {
			//				Util.log("null");
			//			} else {
			//				Util.log(mthd.toString());
			//			}

			// only needed for matching "native"
			//			if (mthd != null)
			//				return mthd.toString();
		}
		String isig = cname + "." + mname + "(";
		if (arg_t.length > 0) {
			isig += arg_t[0];

			for (int i = 1; i < arg_t.length; i++) {
				isig += "," + arg_t[i];
			}
		}
		isig += ")";

		//			Util.log(isig);

		return isig;
	}

	private static boolean apply_permfilter(Instruction instr, PermissionFilter pf, ConstantPoolGen cpgen) {
		boolean ret = false;

		if (instr instanceof InvokeInstruction) {
			InvokeInstruction invoke = (InvokeInstruction) instr;

			String isig = get_invoke_sig(invoke, cpgen);
			// Util.log(isig);

			for (String perm : pf.filters) {
				boolean used = PermissionMap.checkPermUse(isig, perm);

				if (used) {
					Util.log("Matched: " + isig + " <==> " + perm);
					ret = true;
					break;
				}
			}
		}

		return ret;
	}

	private static boolean apply_classfilter(JavaClass jcls) {
		boolean matched = true;

		// 0. check class name
		if (cf.nameFilter != null) {
			matched = jcls.getClassName().matches(cf.nameFilter);
		}

		if (matched && cf.hierachyFilter != null) {
			String cname;
			matched = false;

			// 1. check all interfaces implemented by this class (directly or indirectly)
			try {
				for (JavaClass iface_jcls : jcls.getAllInterfaces()) {
					cname = iface_jcls.getClassName();
					if (cname.matches(cf.hierachyFilter)) {
						matched = true;
						break;
					}
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}

			if (!matched) {
				// 2. check all super classes in this class's inheritance tree 
				try {
					for (JavaClass super_jcls : jcls.getSuperClasses()) {
						cname = super_jcls.getClassName();
						if (cname.matches(cf.hierachyFilter)) {
							matched = true;
							break;
						}
					}
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}

		return matched;
	}

	// get file name for all classes
	private static List<String> get_all_class_fn(String cls_dir) {
		List<String> class_fns = new ArrayList<String>();

		try {
			Process proc = Runtime.getRuntime().exec("find " + cls_dir + " -name *.class");
			BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()), 16);
			String line;
			while ((line = br.readLine()) != null) {
				class_fns.add(line.trim());
			}
			proc.waitFor();
		} catch (Throwable t) {
			t.printStackTrace();
		}

		return class_fns;
	}
}

class ClassFilter {
	public String nameFilter, hierachyFilter;

	public boolean isSet() {
		return (nameFilter != null || hierachyFilter != null);
	}

	public String toString() {
		return "ClassFilter(" + (nameFilter == null ? "null" : nameFilter) + "," + (hierachyFilter == null ? "null" : hierachyFilter) + ")";
	}
}

class MethodFilter {
	public String nameFilter;

	public boolean isSet() {
		return (nameFilter != null);
	}

	public String toString() {
		return "MethodFilter(" + (nameFilter == null ? "null" : nameFilter) + ")";
	}
}

class BytecodeFilter {
	public BytecodePosition pos;
	public short type;
	public String filter;

	public boolean isSet() {
		return (pos != null || type > 0 || (filter != null && filter.length() > 0));
	}

	public String toString() {
		String s = "null";

		if (pos != null) {
			s = String.valueOf(pos.getValue());
		}

		if (type > 0) {
			s = String.valueOf(type);
		}

		if (filter != null) {
			s += "," + filter;
		}

		return "BytecodeFilter(" + s + ")";
	}
}

class PermissionFilter {
	public String[] filters;

	public boolean isSet() {
		return (filters != null && filters.length > 0);
	}

	public String toString() {
		String s = "";

		if (filters == null) {
			s = "null";
		} else {
			s += filters[0];
			for (int i = 1; i < filters.length; i++) {
				s += ", " + filters[i];
			}
		}

		return "PermissionFilter(" + s + ")";
	}
}
