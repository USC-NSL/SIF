package enl.sif.path;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

public class CallSite implements Serializable {
	private static final long serialVersionUID = 3809169145599571922L;
	public int mid, bcos;
	public Set<Integer> callees;

	public CallSite(int mid, int bcos, Set<Integer> callees) {
		this.mid = mid;
		this.bcos = bcos;
		this.callees = callees;
	}

	// mid and position should uniquely identify a call site
	// callees are for the ease of static analysis

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + bcos;
		result = prime * result + mid;
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
		CallSite other = (CallSite) obj;
		if (bcos != other.bcos)
			return false;
		if (mid != other.mid)
			return false;
		return true;
	}

	public String toString() {
		String s = "";

		for (Iterator<Integer> iter = callees.iterator(); iter.hasNext();) {
			int mid = iter.next();
			s += " " + mid;
		}

		s = "[" + s.trim() + "]";

		return "CallSite: (" + mid + ", " + bcos + ") --> " + s;
	}
}
