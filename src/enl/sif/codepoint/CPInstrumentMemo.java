package enl.sif.codepoint;

public class CPInstrumentMemo {
	public CP cp;
	public InstrumentOperation op;
	public InstrumentPosition pos;
	public UserDefinedInvoke code;

	public CPInstrumentMemo(CP cp, InstrumentOperation op, InstrumentPosition pos, UserDefinedInvoke code) {
		this.cp = cp;
		this.op = op;
		this.pos = pos;
		this.code = code;
	}

	public String toString() {
		return "{" + cp + "," + op + "," + pos + "," + code + "}";
	}
}
