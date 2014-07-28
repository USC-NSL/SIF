package enl.sif.codepoint;

/***
 * This class represents Code Point which uniquely identifies a position in the
 * collection of bytecodes. It consists of <global_method_id, bytecode_offset>.
 * 
 * @author haos@enl.usc.edu
 * 
 */
public class CP {
	public int mid, bcos;

	public CP(int mid, int bcos) {
		this.mid = mid;
		this.bcos = bcos;
	}

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
		CP other = (CP) obj;
		if (bcos != other.bcos)
			return false;
		if (mid != other.mid)
			return false;
		return true;
	}

	public String toString() {
		return "CP<" + mid + ", " + bcos + ">";
	}
}
