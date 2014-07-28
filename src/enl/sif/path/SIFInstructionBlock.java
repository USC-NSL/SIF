package enl.sif.path;

import java.util.List;

import org.apache.bcel.generic.InstructionHandle;

public class SIFInstructionBlock {
	private int mid, cs;
	private SIFInstruction first, last;

	public SIFInstructionBlock(int mid, List<InstructionHandle> instrs) {
		this.mid = mid;
		this.cs = -1;
		SIFInstruction curr = new SIFInstruction(instrs.get(0));
		this.first = curr;

		for (int i = 1; i < instrs.size(); i++) {
			SIFInstruction next = new SIFInstruction(instrs.get(i));
			curr.setNext(next);
			curr = next;
		}

		this.last = curr;
	}

	public SIFInstruction locateSIFInstruction(int pos) {
		SIFInstruction curr = first;

		while (curr != null) {
			int _pos = curr.getInstructionHandle().getPosition();
			if (_pos == pos) {
				return curr;
			}
			curr = curr.getNext();
		}
		return null;
	}

	public int getCS() {
		return cs;
	}

	public void setCS(int cs) {
		this.cs = cs;
	}

	public int getMid() {
		return mid;
	}

	public void setMid(int mid) {
		this.mid = mid;
	}

	public SIFInstruction getFirst() {
		return first;
	}

	public void setFirst(SIFInstruction first) {
		this.first = first;
	}

	public SIFInstruction getLast() {
		return last;
	}

	public void setLast(SIFInstruction last) {
		this.last = last;
	}

	public String toString() {
		return first.toString();
	}
}
