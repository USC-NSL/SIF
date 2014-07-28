package enl.sif.codepoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

public class ReflectionHandler {
	public static void run() {
		String dir = "/home/haos/workspace/HelloWorld/bin/classes";

		for (String class_fn : get_all_class_fn(dir)) {
			JavaClass jcls = null;

			try {
				jcls = new ClassParser(class_fn).parse();
			} catch (ClassFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			assert jcls != null : "JavaClass is NULL.";
			ClassGen cgen = new ClassGen(jcls);
			InstructionFactory inf = new InstructionFactory(cgen);

			for (Method method : jcls.getMethods()) {
				if (!method.isAbstract() && !method.isInterface() && !method.getName().equals("<clinit>")) {
					ConstantPoolGen cpgen = cgen.getConstantPool();
					MethodGen mgen = new MethodGen(method, cgen.getClassName(), cpgen);
					Method new_method = instrument_reflection(inf, mgen, cpgen);

					if (method != new_method) {
						cgen.replaceMethod(method, new_method);
						save(cgen, class_fn);
					}
				}
			}
		}
	}

	// count number of reflective method invokes for given method
	private static Method instrument_reflection(InstructionFactory inf, MethodGen mgen, ConstantPoolGen cpgen) {
		Method ret = mgen.getMethod();

		InstructionList ilist = mgen.getInstructionList();
		if (ilist == null || ilist.size() == 0)
			return ret;

		InstructionHandle[] ihdls = ilist.getInstructionHandles();

		for (InstructionHandle ihdl : ihdls) {
			Instruction instr = ihdl.getInstruction();

			// go through all instructions and look for invokes
			if (instr instanceof InvokeInstruction) {
				InvokeInstruction invoke = (InvokeInstruction) instr;
				ReferenceType rtype = invoke.getReferenceType(cpgen);

				if (rtype instanceof ObjectType) {
					String cname = ((ObjectType) rtype).getClassName();
					String mname = invoke.getName(cpgen);

					// we look for exact match
					if (cname.equals("java.lang.reflect.Method") && mname.equals("invoke")) {
						// Util.log(rtype.toString());
						Type[] arg_types = { new ObjectType(java.lang.reflect.Method.class.getCanonicalName()), Type.OBJECT, new ArrayType(Type.OBJECT, 1) };
						ihdl.setInstruction(inf.createInvoke(SIFAStub.class.getCanonicalName(), "invoke", Type.OBJECT, arg_types, Constants.INVOKESTATIC));
					}
				} else {
					// reference type can be ArrayType or UninitializedObjectType
				}
			}
		}

		ilist.setPositions();

		mgen.setInstructionList(ilist);
		mgen.removeLineNumbers();
		mgen.setMaxStack();
		mgen.setMaxLocals();

		ret = mgen.getMethod();

		return ret;
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
}
