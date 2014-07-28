package enl.sif.path;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;

import enl.epp.CFG;
import enl.epp.Edge;
import enl.epp.PathRegenerator;
import enl.sif.codepoint.Util;

/***
 * Given a log file of path statistics. Stitch together paths from all methods being invoked.
 * @author haos
 *
 */
public class PathStitcher {
	public static List<Integer> run(List<LogRecord> entries) {
		List<Integer> P = new ArrayList<Integer>(); // list of instructions on stitched path
		Stack<Object> S = new Stack<Object>(); // simulated call stack analysis

		// String path_str = null;

		for (int i = 0; i < entries.size(); i++) {
			LogRecord entry = entries.get(i);

			Util.log("Processing " + entry);

			EntryExitLogRecord eelr = null;
			if (entry instanceof EntryExitLogRecord) {
				eelr = (EntryExitLogRecord) entry;
			}

			if (eelr == null || eelr.getType() != EntryExitLogRecord.EXIT) {
				S.push(entry);
			} else { // stop for analysis when hit the first EXIT record
				int mid = eelr.getMid();

				EntryExitLogRecord tmp_eelr;
				Stack<Object> tmp_S = new Stack<Object>();

				do {
					Object obj = S.pop();

					if (obj instanceof EntryExitLogRecord) {
						tmp_eelr = (EntryExitLogRecord) obj;
					} else {
						tmp_eelr = null;
					}

					tmp_S.add(obj);
				} while ((tmp_eelr == null) || (tmp_eelr.getType() != EntryExitLogRecord.ENTRY));

				tmp_S.pop(); // discard ENTRY record
				assert (!tmp_S.isEmpty()) : "There should be at least one path segment between entry and exit";

				Queue<SIFInstructionBlock> Q = new LinkedList<SIFInstructionBlock>();
				PathSegmentLogRecord pslr;

				SIFInstructionBlock sif_iblk;

				while (!tmp_S.isEmpty()) {
					Object obj = tmp_S.pop();

					if (obj instanceof PathSegmentLogRecord) {
						pslr = (PathSegmentLogRecord) obj;
						long pid = pslr.getPid();

						List<InstructionHandle> ilist = get_instr(mid, pid);
						sif_iblk = new SIFInstructionBlock(mid, ilist);
						Queue<Pair<SIFInstruction, SIFInstructionBlock>> tmp_Q = new LinkedList<Pair<SIFInstruction, SIFInstructionBlock>>();
						while (!Q.isEmpty()) {
							SIFInstructionBlock instr_blk = Q.poll();
							int cs = instr_blk.getCS();
							if (cs >= 0) {
								SIFInstruction hook = sif_iblk.locateSIFInstruction(cs);
								assert (hook != null) : "CallSite should exist on path";
								tmp_Q.add(new Pair<SIFInstruction, SIFInstructionBlock>(hook, instr_blk));
							} else {
								assert (Q.isEmpty()) : "Q should be empty by now";
								instr_blk.getLast().setNext(sif_iblk.getFirst());
								sif_iblk.setFirst(instr_blk.getFirst());
							}
						}

						while (!tmp_Q.isEmpty()) {
							Pair<SIFInstruction, SIFInstructionBlock> pair = tmp_Q.poll();
							SIFInstruction hook = pair.getFirst();
							SIFInstructionBlock invoke_blk = pair.getSecond();
							SIFInstruction tmp = hook.getNext();
							hook.setNext(invoke_blk.getFirst());
							invoke_blk.getLast().setNext(tmp);
						}

						Q.add(sif_iblk);
					} else if (obj instanceof SIFInstructionBlock) {
						sif_iblk = (SIFInstructionBlock) obj;
						Q.add(sif_iblk);
					} else {
						// UNKNOWN type
					}
				}

				SIFInstructionBlock instr_blk = Q.poll();

				if (!S.isEmpty()) {
					Object obj = S.peek();
					if (obj instanceof CallSiteLogRecord) {
						CallSiteLogRecord cslr = (CallSiteLogRecord) S.pop();
						instr_blk.setCS(cslr.getCS());
					}
				}
				S.push(instr_blk);

				//				String tmp_str = "(" + mid + "," + pid;
				//				get_instr(mid, pid);
				//
				//				while (!tmp_S.isEmpty()) { // case for methods with loops: there are multiple path segments
				//					tmp_entry = tmp_S.pop();
				//					pid = tmp_entry.getPid();
				//					tmp_str += "-" + pid;
				//					get_instr(mid, pid);
				//				}
				//
				//				tmp_str += ")";
				//
				//				path_str = (path_str == null) ? tmp_str : tmp_str + " --> " + path_str;
			}
		}
		// assert (S.isEmpty()) : "Call stack should be empty at end";

		// Util.log(path_str);
		assert (S.size() == 1) : "Call stack should have one final instruction block at end";
		SIFInstructionBlock sif_iblk = (SIFInstructionBlock) S.pop();
		Util.log(sif_iblk.toString());

		return P;
	}

	@SuppressWarnings("unchecked")
	private static List<InstructionHandle> get_instr(int mid, long pid) {
		Util.log("-----------------------------------instr for (" + mid + ", " + pid + ")");
		List<InstructionHandle> ret = new ArrayList<InstructionHandle>();

		String prefix = "/var/tmp/sifa/path/reconstruction/";
		CFG dag = (CFG) Util.readFromFile(prefix + "epp_data/" + mid + ".dag");
		Hashtable<Edge, Long> labeled_edge = (Hashtable<Edge, Long>) Util.readFromFile(prefix + "epp_data/" + mid + ".le");
		Hashtable<Edge, Long> ldde = (Hashtable<Edge, Long>) Util.readFromFile(prefix + "epp_data/" + mid + ".ldde");
		InstructionList ilist = (InstructionList) Util.readFromFile(prefix + "epp_data/" + mid + ".ilist");

		PathRegenerator pr = new PathRegenerator(dag, labeled_edge, ldde);
		List<Integer> path = pr.findPath(pid);
		Util.log(path.toString());
		// for no path sensitivity calculation

		for (int j = 0; j < path.size(); j++) {
			InstructionHandle ihdl = ilist.findHandle(path.get(j));
			ret.add(ihdl);
		}

		// Util.log(new SIFInstructionBlock(mid, ret).toString());

		return ret;
	}
}

class Pair<T1, T2> {
	private T1 first;
	private T2 second;

	public Pair(T1 t1, T2 t2) {
		this.first = t1;
		this.second = t2;
	}

	public T1 getFirst() {
		return first;
	}

	public T2 getSecond() {
		return second;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((first == null) ? 0 : first.hashCode());
		result = prime * result + ((second == null) ? 0 : second.hashCode());
		return result;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Pair other = (Pair) obj;
		if (first == null) {
			if (other.first != null)
				return false;
		} else if (!first.equals(other.first))
			return false;
		if (second == null) {
			if (other.second != null)
				return false;
		} else if (!second.equals(other.second))
			return false;
		return true;
	}

	public String toString() {
		return "Pair(" + first + "," + second + ")";
	}
}
