package enl.sif.path;

public class EntryExitLogRecord extends LogRecord {
	private boolean type;
	private int mid;
	public static boolean ENTRY = true, EXIT = false;

	public EntryExitLogRecord(boolean type, int mid, short tid) {
		super(tid);
		this.type = type;
		this.mid = mid;
	}

	public int getMid() {
		return mid;
	}

	public boolean getType() {
		return type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + mid;
		result = prime * result + (type ? 1231 : 1237);
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
		EntryExitLogRecord other = (EntryExitLogRecord) obj;
		if (mid != other.mid)
			return false;
		if (type != other.type)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return (type ? "ENTRY " : "EXIT ") + mid;
	}

}
