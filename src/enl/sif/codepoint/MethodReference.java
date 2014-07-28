package enl.sif.codepoint;

import java.io.Serializable;

public class MethodReference implements Serializable {
	private static final long serialVersionUID = 2765623183357195950L;
	public String class_name, mthd_sig;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((class_name == null) ? 0 : class_name.hashCode());
		result = prime * result + ((mthd_sig == null) ? 0 : mthd_sig.hashCode());
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
		MethodReference other = (MethodReference) obj;
		if (class_name == null) {
			if (other.class_name != null)
				return false;
		} else if (!class_name.equals(other.class_name))
			return false;
		if (mthd_sig == null) {
			if (other.mthd_sig != null)
				return false;
		} else if (!mthd_sig.equals(other.mthd_sig))
			return false;
		return true;
	}

	public MethodReference(String class_name, String mthd_sig) {
		this.class_name = class_name;
		this.mthd_sig = mthd_sig;
	}

	public String toString() {
		return class_name + "," + mthd_sig;
	}
}