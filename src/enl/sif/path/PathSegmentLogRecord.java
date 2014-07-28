package enl.sif.path;

public class PathSegmentLogRecord extends LogRecord {
	private short fid; // flow id
	private long pid; // path segment id

	public PathSegmentLogRecord(short fid, long pid, short tid) {
		super(tid);
		this.fid = fid;
		this.pid = pid;
	}

	public short getFid() {
		return fid;
	}

	public long getPid() {
		return pid;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + fid;
		result = prime * result + (int) (pid ^ (pid >>> 32));
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
		PathSegmentLogRecord other = (PathSegmentLogRecord) obj;
		if (fid != other.fid)
			return false;
		if (pid != other.pid)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "PS " + pid;
	}

}
