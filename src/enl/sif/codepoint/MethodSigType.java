package enl.sif.codepoint;

/***
 * This class represents user's desire to access method signatures.
 * It expects no arguments and will log the internal method id. 
 * Users are expected to consult the instrumentation framework to 
 * convert it back to actual method signatures. 
 * 
 * @author haos
 *
 */
public class MethodSigType extends AccessType {
	public String toString() {
		return "MethodSigType";
	}
}
