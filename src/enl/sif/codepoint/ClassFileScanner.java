package enl.sif.codepoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;

public class ClassFileScanner {
	private String dir;

	public ClassFileScanner(String dir) {
		this.dir = dir;
	}

	// build method map
	public void scan() {
		for (String class_fn : getAllClassFileNames(dir)) {
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

			for (Method method : jcls.getMethods()) {
				if (!method.isAbstract() && !method.isInterface() && !method.getName().equals("<clinit>")) {
					// only scan methods that are can be executed in real life
					String mthd_sig = method.getName() + ":" + method.getSignature();
					MethodMap.addMethodSig(cgen.getClassName(), mthd_sig);

					// temporary
					// int cnt = check_reflection(new MethodGen(method, cgen.getClassName(), cgen.getConstantPool()), cgen.getConstantPool());
					// if (cnt > 0) {
					//	Util.log(method.toString() + " " + cnt);
					// }
				}
			}
		}

		MethodMap.save(dir + "/../sifa/method.map");
	}

	// count number of reflective method invokes for given method
	private int check_reflection(MethodGen mgen, ConstantPoolGen cpgen) {
		int cnt = 0;

		InstructionList ilist = mgen.getInstructionList();
		if (ilist == null || ilist.size() == 0)
			return cnt;

		for (Instruction instr : ilist.getInstructions()) {
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
						cnt++;
					}
				} else {
					// reference type can be ArrayType or UninitializedObjectType
				}
			}
		}

		return cnt;
	}

	// get file name for all classes
	public static List<String> getAllClassFileNames(String cls_dir) {
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
