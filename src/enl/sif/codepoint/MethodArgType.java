package enl.sif.codepoint;

public class MethodArgType extends AccessType {
	private int[] arg_idx;
	private boolean all_args;
	public static MethodArgType ALL_ARGS = new MethodArgType();

	public MethodArgType() {
		this.all_args = true;
	}

	// we should allow user to express which method argument to use: 0 ~ n-1
	public MethodArgType(int... idx) {
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
		sb.append("MethodArgType(");

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
