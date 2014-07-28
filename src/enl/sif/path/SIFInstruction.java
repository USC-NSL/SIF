package enl.sif.path;

import org.apache.bcel.generic.InstructionHandle;

public class SIFInstruction {
	private InstructionHandle ihdl;
	private SIFInstruction next;

	public SIFInstruction(InstructionHandle ihdl) {
		this.ihdl = ihdl;
	}

	public InstructionHandle getInstructionHandle() {
		return this.ihdl;
	}

	public SIFInstruction getNext() {
		return next;
	}

	public void setNext(SIFInstruction next) {
		this.next = next;
	}

	public String toString() {
		// Instruction instr = ihdl.getInstruction();
		// return (next == null) ? instr.getName() : instr.getName() + " -> " + next;
		return (next == null) ? ihdl.getPosition() + "" : ihdl.getPosition() + " -> " + next;
	}
}
