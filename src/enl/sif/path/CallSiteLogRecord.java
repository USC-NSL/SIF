package enl.sif.path;

public class CallSiteLogRecord extends LogRecord {
	private int cs;

	public CallSiteLogRecord(int cs, short tid) {
		super(tid);
		this.cs = cs;
	}

	public int getCS() {
		return cs;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + cs;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CallSiteLogRecord other = (CallSiteLogRecord) obj;
		if (cs != other.cs)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "CS " + cs;
	}

}
