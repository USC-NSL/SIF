package enl.sif.codepoint;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LocalVariableGen;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.PUSH;
import org.apache.bcel.generic.Type;

public class UserDefinedInvoke {
	private String class_name, method_name;
	private AccessType[] args;

	public UserDefinedInvoke(String class_name, String method_name, AccessType... args) {
		this.class_name = class_name;
		this.method_name = method_name;
		this.args = args;
	}

	public String getClassName() {
		return class_name;
	}

	public String getMethodName() {
		return method_name;
	}

	public boolean requiresMethodArgAccess() {
		boolean ret = false;

		if (args != null && args.length > 0) {
			for (AccessType at : args) {
				if (at instanceof MethodArgType) {
					ret = true;
					break;
				}
			}
		}

		return ret;
	}

	public boolean requiresInstructionArgAccess() {
		boolean ret = false;

		if (args != null && args.length > 0) {
			for (AccessType at : args) {
				if (at instanceof InstructionArgType) {
					ret = true;
					break;
				}
			}
		}

		return ret;
	}

	public InstructionList getInstructions(InstructionFactory inf, CP cp, MethodGen mgen, ConstantPoolGen cpgen, InstructionHandle ihdl) {
		int mid = cp.mid;
		int pos = cp.bcos;

		InstructionList ilist = new InstructionList();
		Instruction instr = (ihdl == null) ? new NOP() : ihdl.getInstruction();

		Type[] mthd_arg_types = mgen.getArgumentTypes();
		Type[] instr_arg_types = null;

		// 0. get instruction argument types
		// special case: INVOKE consumes "UNPREDICTABLE" number of words
		if (instr instanceof InvokeInstruction) {
			InvokeInstruction invoke = (InvokeInstruction) instr;
			instr_arg_types = invoke.getArgumentTypes(cpgen);
			// invoke.getReferenceType(cpgen);
		} else if (instr.consumeStack(cpgen) > 0) { // e.g. iadd consumes 2 words, ladd consumes 4 words
			// TODO: find argument types for generic instruction			
		} else {
			// ERROR: user asks for argument access to instructions that do not consume arguments 
		}

		// 1. prepare instruction list for access to: a). method arguments; b). instruction arguments; c). this argument
		InstructionList mthd_sig_ilist = new InstructionList();
		InstructionList mthd_arg_ilist = new InstructionList();
		InstructionList instr_arg_ilist = new InstructionList();
		InstructionList this_arg_ilist = new InstructionList();
		InstructionList cp_ilist = new InstructionList();

		for (AccessType arg : args) {
			if (arg instanceof MethodArgType) {
				MethodArgType mat = (MethodArgType) arg;

				if (mat.useAllArgs()) { // user asked for all arguments
					int idx = mgen.isStatic() ? 0 : 1;
					for (int i = 0; i < mthd_arg_types.length; i++) {
						mthd_arg_ilist.append(InstructionFactory.createLoad(mthd_arg_types[i], idx));
						idx++;
					}
				} else { // user specified a list of arguments to access
					for (int i : mat.getArgs()) {
						int idx = mgen.isStatic() ? i : i + 1;
						mthd_arg_ilist.append(InstructionFactory.createLoad(mthd_arg_types[i], idx));
					}
				}
			} else if (arg instanceof InstructionArgType) {
				InstructionArgType iat = (InstructionArgType) arg;

				// save current top n values on stack to local variables
				LocalVariableGen[] lvgs = new LocalVariableGen[instr_arg_types.length];
				for (int i = instr_arg_types.length - 1; i >= 0; i--) {
					Type T = instr_arg_types[i];
					// TODO: "null" can be changed to previous instruction
					lvgs[i] = mgen.addLocalVariable(String.valueOf(i), T, null, ihdl);
					instr_arg_ilist.append(InstructionFactory.createStore(T, lvgs[i].getIndex()));
				}

				// restore the stack by reloading n values
				for (int i = 0; i < instr_arg_types.length; i++) {
					Type T = instr_arg_types[i];
					instr_arg_ilist.append(InstructionFactory.createLoad(T, lvgs[i].getIndex()));
				}

				// load variables selected by user
				if (iat.useAllArgs()) {
					for (int i = 0; i < instr_arg_types.length; i++) {
						Type T = instr_arg_types[i];
						instr_arg_ilist.append(InstructionFactory.createLoad(T, lvgs[i].getIndex()));
					}
				} else {
					for (int i : iat.getArgs()) {
						Type T = instr_arg_types[i];
						instr_arg_ilist.append(InstructionFactory.createLoad(T, lvgs[i].getIndex()));
					}
				}
			} else if (arg instanceof ThisType) {
				this_arg_ilist.append(InstructionFactory.THIS);
			} else if (arg instanceof MethodSigType) {
				mthd_sig_ilist.append(new PUSH(cpgen, mid));
			} else if (arg instanceof CodePointType) {
				cp_ilist.append(new PUSH(cpgen, mid));
				cp_ilist.append(new PUSH(cpgen, pos));
			} else {
				// ERROR: unknown AccessType
			}
		} // end for

		// 2. insert the list instructions in order
		if (!instr_arg_ilist.isEmpty()) { // instruction arguments
			ilist.append(instr_arg_ilist);
		}

		if (!mthd_sig_ilist.isEmpty()) { // method signature
			ilist.append(mthd_sig_ilist);
		}

		if (!mthd_arg_ilist.isEmpty()) { // method arguments
			ilist.append(mthd_arg_ilist);
		}

		if (!this_arg_ilist.isEmpty()) { // *this* argument
			ilist.append(this_arg_ilist);
		}

		if (!cp_ilist.isEmpty()) { // codepoint argument
			ilist.append(cp_ilist);
		}

		// 3. append the invoke to user-defined function
		// ilist.append(inf.createInvoke(this.class_name, this.method_name, Type.VOID, Type.NO_ARGS, Constants.INVOKESTATIC));

		// for timing profiling: mid, position
		// ilist.append(inf.createInvoke(this.class_name, this.method_name, Type.VOID, new Type[] { Type.INT, Type.INT }, Constants.INVOKESTATIC));

		// for fine-grain permission
		ilist.append(inf.createInvoke(this.class_name, this.method_name, Type.VOID, new Type[] { Type.OBJECT }, Constants.INVOKESTATIC));

		// for flurry-like analytics
		// ilist.append(inf.createInvoke(this.class_name, this.method_name, Type.VOID, new Type[] { Type.INT }, Constants.INVOKESTATIC));

		// TODO: how can users specify the arguments (#args and types)? 
		// ilist.append(inf.createInvoke(this.class_name, this.method_name, Type.VOID, instr_arg_types, Constants.INVOKESTATIC));

		return ilist;
	}

	public String toString() {
		String s = "";
		if (this.args != null) {
			for (AccessType at : this.args) {
				s += at.toString();
			}
		}

		return "<" + this.class_name + "." + this.method_name + "(" + s + ")>";
	}
}
