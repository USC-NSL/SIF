package enl.sif.codepoint;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InstructionTargeter;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.POP;
import org.apache.bcel.generic.Type;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;

import enl.sif.Config;

public class CPInstrumenter {
	private static List<CPInstrumentMemo> todos = new ArrayList<CPInstrumentMemo>();

	// just put it in to-do list and do it in exec() 
	public static void dryRun(CP cp, InstrumentOperation op, InstrumentPosition pos, UserDefinedInvoke code) {
		todos.add(new CPInstrumentMemo(cp, op, pos, code));
	}

	public static void exec() {
		Util.log(todos.toString());

		// 0. organize memos by method so that all instrumentation can be done in one pass
		Map<Integer, List<CPInstrumentMemo>> new_todos = new HashMap<Integer, List<CPInstrumentMemo>>();
		List<CPInstrumentMemo> memos;
		for (CPInstrumentMemo todo : todos) {
			CP cp = todo.cp;

			if (new_todos.containsKey(cp.mid)) {
				memos = new_todos.get(cp.mid);
			} else {
				memos = new ArrayList<CPInstrumentMemo>();
			}

			memos.add(todo);
			new_todos.put(cp.mid, memos);
		}

		Util.log(new_todos.toString());

		// IMPORTANT: store todos in the class level
		Map<String, Map<Integer, List<CPInstrumentMemo>>> final_todos = new HashMap<String, Map<Integer, List<CPInstrumentMemo>>>();
		for (Iterator<Entry<Integer, List<CPInstrumentMemo>>> iter = new_todos.entrySet().iterator(); iter.hasNext();) {
			Entry<Integer, List<CPInstrumentMemo>> entry = iter.next();
			int mid = entry.getKey();
			memos = entry.getValue();

			String cname = MethodMap.getClassName(mid);
			Map<Integer, List<CPInstrumentMemo>> mthd_todos = (final_todos.containsKey(cname)) ? final_todos.get(cname) : new HashMap<Integer, List<CPInstrumentMemo>>();

			assert (!mthd_todos.containsKey(mid)) : "A method can only be processed ONCE";

			mthd_todos.put(mid, memos);
			final_todos.put(cname, mthd_todos);
		}

		// 1. go through each class and do the instrumentation
		for (Iterator<Entry<String, Map<Integer, List<CPInstrumentMemo>>>> iter = final_todos.entrySet().iterator(); iter.hasNext();) {
			Entry<String, Map<Integer, List<CPInstrumentMemo>>> entry = iter.next();
			String cname = entry.getKey();
			Map<Integer, List<CPInstrumentMemo>> mthd_todos = entry.getValue();

			String class_fn = Config.INPUT_DIR + cname.replace('.', '/') + ".class";
			JavaClass jcls = Util.loadJavaClass(class_fn);
			ClassGen cgen = new ClassGen(jcls);

			for (Iterator<Entry<Integer, List<CPInstrumentMemo>>> it = mthd_todos.entrySet().iterator(); it.hasNext();) {
				Entry<Integer, List<CPInstrumentMemo>> en = it.next();
				int mid = en.getKey();
				memos = en.getValue();

				String msig = MethodMap.getMethodSig(mid);
				String[] vals = msig.split(":");
				Method mthd = cgen.containsMethod(vals[0], vals[1]);

				assert (mthd != null) : "Method is NULL";

				MethodGen mgen = new MethodGen(mthd, cgen.getClassName(), cgen.getConstantPool());
				Method new_mthd = exec_memo(cgen, mgen, mid, memos);
				cgen.replaceMethod(mthd, new_mthd);
			}

			// now it's SAFE to output .class file
			save(cgen, class_fn);
		}

	}

	private static Method exec_memo(ClassGen cgen, MethodGen mgen, int mid, List<CPInstrumentMemo> memos) {// CP cp, InstrumentOperation op, InstrumentPosition pos, UserDefinedInvoke code) {
		// 0. find and load target class file
		//		String cname = MethodMap.getClassName(mid);
		//		Util.log(cname);
		//		String class_fn = Config.INPUT_DIR + cname.replace('.', '/') + ".class";
		//		JavaClass jcls = null;
		//
		//		try {
		//			jcls = new ClassParser(class_fn).parse();
		//		} catch (Exception e) {
		//			e.printStackTrace();
		//		}
		//
		//		assert (jcls != null) : "JavaClass is NULL";
		//
		//		ClassGen cgen = new ClassGen(jcls);

		ConstantPoolGen cpgen = cgen.getConstantPool();

		// 1. find target method
		//		String msig = MethodMap.getMethodSig(mid);
		//		String[] vals = msig.split(":");
		//		Method mthd = cgen.containsMethod(vals[0], vals[1]);
		//
		//		assert (mthd != null) : "Method is NULL";

		InstructionFactory inf = new InstructionFactory(cgen);
		//		 MethodGen mgen = new MethodGen(mthd, cgen.getClassName(), cgen.getConstantPool());

		// 2. perform instrumentation on target bytecode
		InstructionList ilist = mgen.getInstructionList();
		if (ilist == null) {
			ilist = new InstructionList();
		}

		final InstructionHandle[] ihdls = ilist.getInstructionHandles();
		// InstructionList ilist_copy = ilist.copy();
		// ilist_copy.setPositions();
		// Util.log(ilist_copy.toString());

		for (CPInstrumentMemo memo : memos) {
			InstrumentOperation op = memo.op;
			InstrumentPosition pos = memo.pos;
			UserDefinedInvoke code = memo.code;
			int bcos = memo.cp.bcos;
			Util.log("Handling " + memo.cp);

			BytecodePosition bcpos;

			switch (op) {
			case INSERT:
				switch (pos) {
				case BEFORE:
					bcpos = new BytecodePosition(bcos);
					if (bcpos.isSpecialPosition()) {
						// TODO: need to build CFG to determine EXIT node
						// first instruction is guaranteed to be after ENTRY
						// last instruction is NOT guaranteed to be before EXIT e.g. last one is GOTO 

						if (bcpos.getValue() == BytecodePosition.ENTRY.getValue()) {
							InstructionList usercode = code.getInstructions(inf, memo.cp, mgen, cpgen, null);
							Util.log("Usercode: haha " + usercode);
							ilist.insert(usercode);
						} else if (bcpos.getValue() == BytecodePosition.EXIT.getValue()) {
							;
						}
					} else {
						// scan through method body to find target bytecode
						InstructionHandle ihdl = find_ihdl(ihdls, bcos);
						assert (ihdl != null) : "Failed to find target bytecode at " + bcos;

						InstructionList usercode = code.getInstructions(inf, memo.cp, mgen, cpgen, ihdl);
						Util.log("Usercode: " + usercode);

						// update pointer to ihdl
						InstructionTargeter[] targeters = ihdl.getTargeters();
						for (InstructionTargeter targeter : targeters) {
							targeter.updateTarget(ihdl, usercode.getStart());
						}

						// do the insert 
						ilist.insert(ihdl, usercode);
					}

					break;
				case AFTER:
					bcpos = new BytecodePosition(bcos);
					if (bcpos.isSpecialPosition()) {
						// TODO: need to build CFG to determine EXIT node
						// first instruction is guaranteed to be after ENTRY
						// last instruction is NOT guaranteed to be before EXIT e.g. last one is GOTO 

						if (bcpos.getValue() == BytecodePosition.ENTRY.getValue()) {
							;
						} else if (bcpos.getValue() == BytecodePosition.EXIT.getValue()) {
							;
						}
					} else {
						// scan through method body to find target bytecode
						InstructionHandle ihdl = find_ihdl(ihdls, bcos);
						assert (ihdl != null) : "Failed to find target bytecode at " + bcos;

						InstructionList usercode = code.getInstructions(inf, memo.cp, mgen, cpgen, ihdl);
						Util.log("Usercode: " + usercode);

						// no need to update targeters

						ilist.append(ihdl, usercode);
					}

					break;
				default: // unsupported locations
				}
				break;
			case UPDATE:
				switch (pos) {
				case AT:
					InstructionHandle ihdl = find_ihdl(ihdls, bcos);
					assert (ihdl != null) : "Failed to find target bytecode at " + bcos;

					// InvokeInstruction userinstr = inf.createInvoke(code.getClassName(), code.getMethodName(), Type.OBJECT, new Type[] { Type.OBJECT }, Constants.INVOKESTATIC);
					InvokeInstruction userinstr = inf.createInvoke(code.getClassName(), code.getMethodName(), new ObjectType(HttpResponse.class.getCanonicalName()), new Type[] { new ObjectType(
							HttpUriRequest.class.getCanonicalName()) }, Constants.INVOKESTATIC);
					ihdl.setInstruction(userinstr);

					ilist.append(ihdl, new POP());
					Util.log("Usercode: " + ihdl);
					break;
				default: // unsupported locations
				}
				break;
			}
		}

		// 3. finalize instrumentation
		ilist.setPositions();

		// Util.log(ilist.toString());

		mgen.setInstructionList(ilist);
		mgen.removeLineNumbers();
		mgen.setMaxStack();
		mgen.setMaxLocals();

		// 4. update and store new class file
		return mgen.getMethod();

		// cgen.replaceMethod(mthd, mgen.getMethod());
		// save(cgen, class_fn);
	}

	private static void save(ClassGen cgen, String class_fn) {
		File out_file = new File(class_fn.replaceFirst("classes", "shao"));
		File ofp = out_file.getParentFile();
		if (!ofp.exists()) {
			ofp.mkdirs();
		}

		try {
			FileOutputStream fos = new FileOutputStream(out_file);
			cgen.getJavaClass().dump(fos);
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static InstructionHandle find_ihdl(InstructionHandle[] ihdls, int offset) {
		for (InstructionHandle ihdl : ihdls) {
			if (ihdl.getPosition() == offset) {
				return ihdl;
			}
		}

		return null;
	}
}
