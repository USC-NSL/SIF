package enl.sif.codepoint;

public class BytecodePosition {
	public static final BytecodePosition ENTRY = new BytecodePosition(-1);
	public static final BytecodePosition EXIT = new BytecodePosition(Integer.MAX_VALUE);

	private int pos;

	public BytecodePosition(int pos) {
		this.pos = pos;
	}

	public boolean isSpecialPosition() {
		return (pos == ENTRY.getValue() || pos == EXIT.getValue());
	}

	public int getValue() {
		return this.pos;
	}

	public String toString() {
		String ret = String.valueOf(pos);

		if (pos == ENTRY.getValue()) {
			ret = "ENTRY";
		} else if (pos == ENTRY.getValue()) {
			ret = "EXIT";
		}

		return ret;
	}
}
