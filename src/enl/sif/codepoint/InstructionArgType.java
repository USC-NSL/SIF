package enl.sif.codepoint;

public class InstructionArgType extends AccessType {
	private int[] arg_idx;
	private boolean all_args;
	public static InstructionArgType ALL_ARGS = new InstructionArgType();
	public static int OBJREF = -1; // used to refer to object reference in invoke instructions (like -1 for invoke argument)
	public static int RETVAL = Integer.MAX_VALUE; // used to refer to value returned by any instructions that are supposed to leave a value on the stack (e.g. invoke, add)

	public InstructionArgType() {
		this.all_args = true;
	}

	// we should allow user to express which instruction argument to use: 0 ~ n-1
	public InstructionArgType(int... idx) {
		this.arg_idx = idx;
		this.all_args = false;
	}

	public boolean useAllArgs() {
		return this.all_args;
	}

	public int[] getArgs() {
		return this.arg_idx;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("InstructionArgType(");

		if (all_args) {
			sb.append("ALL");
		} else {
			if (arg_idx != null && arg_idx.length > 0) {
				sb.append(arg_idx[0]);
				for (int i = 1; i < arg_idx.length; i++) {
					sb.append(", " + arg_idx[i]);
				}
			}
		}

		sb.append(")");

		return sb.toString();
	}
}
