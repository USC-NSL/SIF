package enl.sif.path;

public abstract class LogRecord {
	private short tid; // every entry should have a thread id

	public LogRecord(short tid) {
		this.tid = tid;
	}

	public short getTid() {
		return tid;
	}

	abstract public int hashCode();

	abstract public boolean equals(Object obj);

	abstract public String toString();
}
