package enl.sif.path;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.MethodGen;

import enl.epp.CFG;
import enl.epp.CFGBuilder;
import enl.epp.Util;
import enl.sif.Config;
import enl.sif.codepoint.CP;
import enl.sif.codepoint.MethodMap;

public class SuspectMethodFinder {
	private Map<Integer, Map<Integer, Set<Integer>>> cs_map; // call site graph: method_id --> <bytecode_offset, list of call sites>
	private CP src, dst;

	private void init_map(Set<CallSite> graph) {
		cs_map = new HashMap<Integer, Map<Integer, Set<Integer>>>();

		for (Iterator<CallSite> iter = graph.iterator(); iter.hasNext();) {
			CallSite cs = iter.next();
			int mid = cs.mid;
			Map<Integer, Set<Integer>> cs_set = (cs_map.containsKey(mid)) ? cs_map.get(mid) : new HashMap<Integer, Set<Integer>>();

			cs_set.put(cs.bcos, cs.callees);
			cs_map.put(mid, cs_set);
		}
	}

	public SuspectMethodFinder(Set<CallSite> graph, CP src, CP dst) {
		init_map(graph);
		this.src = src;
		this.dst = dst;
	}

	// convert call site info into directed graph
	private Map<Integer, Set<Integer>> callsites2graph() {
		Map<Integer, Set<Integer>> graph = new HashMap<Integer, Set<Integer>>(); // method_id --> list of callee method ids

		for (Iterator<Entry<Integer, Map<Integer, Set<Integer>>>> iter = cs_map.entrySet().iterator(); iter.hasNext();) {
			Entry<Integer, Map<Integer, Set<Integer>>> entry = iter.next();
			int caller = entry.getKey();
			Map<Integer, Set<Integer>> callsites = entry.getValue();

			Set<Integer> callees = new HashSet<Integer>();

			for (Iterator<Entry<Integer, Set<Integer>>> it = callsites.entrySet().iterator(); it.hasNext();) {
				Entry<Integer, Set<Integer>> en = it.next();
				callees.addAll(en.getValue());
			}

			graph.put(caller, callees);
		}

		//		Util.log("graph: 466: " + graph.get(466));
		//		Util.log("graph: 137: " + graph.get(137));
		//		Util.log("graph: 6733: " + graph.get(6733));

		return graph;
	}

	//	private Map<Integer, Set<Integer>> test() {
	//		Map<Integer, Set<Integer>> graph = new HashMap<Integer, Set<Integer>>();
	//		Set<Integer> callees;
	//
	//		callees = new HashSet<Integer>();
	//		callees.add(2);
	//		graph.put(1, callees);
	//
	//		callees = new HashSet<Integer>();
	//		callees.add(3);
	//		callees.add(4);
	//		callees.add(5);
	//		graph.put(2, callees);
	//
	//		callees = new HashSet<Integer>();
	//		callees.add(6);
	//		graph.put(3, callees);
	//
	//		callees = new HashSet<Integer>();
	//		callees.add(7);
	//		graph.put(4, callees);
	//
	//		callees = new HashSet<Integer>();
	//		callees.add(8);
	//		graph.put(5, callees);
	//
	//		callees = new HashSet<Integer>();
	//		callees.add(9);
	//		graph.put(6, callees);
	//
	//		callees = new HashSet<Integer>();
	//		callees.add(9);
	//		graph.put(7, callees);
	//
	//		callees = new HashSet<Integer>();
	//		callees.add(1);
	//		graph.put(8, callees);
	//
	//		callees = new HashSet<Integer>();
	//		callees.add(10);
	//		graph.put(9, callees);
	//
	//		for (int i = 1; i <= 10; i++) {
	//			Util.log(i + ": " + graph.get(i));
	//		}
	//
	//		return graph;
	//	}

	// find all methods that may be on an execution path from CP x to y
	public Set<Integer> search() {
		ReachingMethodSolver rms = new ReachingMethodSolver(callsites2graph());
		Set<Integer> candidates = new HashSet<Integer>();
		Set<Integer> res = new HashSet<Integer>();

		rms.run();

		if (src.mid == dst.mid) {
			candidates.add(src.mid);
			return candidates;
		}

		// 1. get all methods that src code point can reach
		Set<SearchNode> C = search_mthd(new SearchNode(src.mid, null), src.bcos);

		Util.log("C: " + C);

		for (Iterator<SearchNode> iter = C.iterator(); iter.hasNext();) {
			SearchNode sn = iter.next();
			res.add(sn.mid);
		}

		Util.log("|res|= " + res.size() + ", " + res);

		// 2. only choose methods that can reach dst code point
		Queue<Integer> Q = new LinkedList<Integer>();
		Q.addAll(res);

		while (!Q.isEmpty()) {
			int n = Q.poll();
			Set<Integer> node_out_set = rms.getOut(n);

			if (node_out_set.contains(dst.mid)) {
				// Util.log("Adding " + n);
				candidates.add(n);

				for (Iterator<Integer> iter = node_out_set.iterator(); iter.hasNext();) {
					int new_n = iter.next();
					if (!Q.contains(new_n)) {
						Q.add(new_n);
					}
				}
			}
		}

		//		for (Iterator<Integer> iter = res.iterator(); iter.hasNext();) {
		//			int node = iter.next();
		//			Set<Integer> node_out_set = rms.getOut(node);
		//
		//			//			if (node_out_set == null)
		//			//				continue;
		//
		//			Util.log(node + "-->out: " + node_out_set);
		//
		//			if (node_out_set.contains(dst.mid)) {
		//				candidates.add(node);
		//			}
		//		}

		return candidates;
	}

	private MethodGen load_mthd(String cname, String msig) {
		JavaClass jcls = null;

		String cls_fn = Config.INPUT_DIR + cname.replace(".", "/") + ".class";
		// Util.log(cls_fn);
		String[] arr = msig.split(":");

		try {
			jcls = new ClassParser(cls_fn).parse();
		} catch (IOException e) {
			e.printStackTrace();
		}

		assert (jcls != null) : "JavaClass is NULL";

		ClassGen cgen = new ClassGen(jcls);
		Method mthd = cgen.containsMethod(arr[0], arr[1]);

		assert (mthd != null) : "Method " + msig + " not in ClassGen";

		MethodGen mgen = new MethodGen(mthd, cname, cgen.getConstantPool());

		return mgen;
	}

	private Set<Integer> find_reachable_set(CFG cfg, int pos) {
		Set<Integer> ret = new HashSet<Integer>();
		Queue<Integer> Q = new LinkedList<Integer>();
		Q.add(pos);
		ret.add(pos);

		while (!Q.isEmpty()) {
			int node = Q.poll();

			List<Integer> children = cfg.getChildren(node);
			Util.log("Children for " + node + ": " + children);
			if (children == null) {
				continue;
			}
			for (int i = 0; i < children.size(); i++) {
				int child = children.get(i);
				if (!ret.contains(child)) {
					Q.add(child);
					ret.add(child);
				}
			}
		}

		ret.remove(CFG.ENTRY);
		ret.remove(CFG.EXIT);

		return ret;
	}

	private CodeExceptionGen find_ceg(CodeExceptionGen[] cegs, int pos) {
		CodeExceptionGen ret = null;
		for (CodeExceptionGen ceg : cegs) {
			int start = ceg.getStartPC().getPosition();
			int end = ceg.getEndPC().getPosition();
			if (start <= pos && pos <= end) {
				ret = ceg;
				break;
			}
		}

		return ret;
	}

	// special case: find set of suspicious methods for given method from non-ENTRY position pos
	// in general, we can simply collect all call sites in the method, if the search starts from ENTRY
	private Set<SearchNode> search_mthd(SearchNode start, int pos) {
		Set<SearchNode> ret = new HashSet<SearchNode>();
		int node_pos = (pos < 0) ? CFG.ENTRY : pos;

		// 0. load method for mid
		int mid = start.mid;
		String cname = MethodMap.getClassName(mid);
		String msig = MethodMap.getMethodSig(mid);
		MethodGen mgen = load_mthd(cname, msig);

		if (mgen.getInstructionList() == null) {
			return ret;
		}

		// 1. build CFG
		CFG cfg = CFGBuilder.run(mgen);
		// Util.log(cfg.toString());

		// 2. start from pos and find set of reachable nodes
		Set<Integer> reachable_nodes = find_reachable_set(cfg, node_pos);
		// SortedSet<Integer> ss = new TreeSet<Integer>(reachable_nodes);
		// Util.log(ss.toString());

		// 3. for each node include entries to exception flows
		CodeExceptionGen[] cegs = mgen.getExceptionHandlers();
		// Util.log(cegs.length);

		Set<CodeExceptionGen> ceg_todos = new HashSet<CodeExceptionGen>();

		for (Iterator<Integer> iter = reachable_nodes.iterator(); iter.hasNext();) {
			int node = iter.next();
			CodeExceptionGen ceg = find_ceg(cegs, node);

			if (ceg != null) {
				ceg_todos.add(ceg);
			}
		}

		if (!ceg_todos.isEmpty()) {
			// Util.log("Have to consider " + ceg_todos.size() + " CEG");
		}

		// 4. repeat until no more new nodes to add
		// TODO: handle ceg_todos

		// 5. filter by CallSite nodes
		if (cs_map.containsKey(mid)) {
			Map<Integer, Set<Integer>> cs_set = cs_map.get(mid);

			for (Iterator<Integer> iter = reachable_nodes.iterator(); iter.hasNext();) {
				int node = iter.next();
				// Util.log("Consider " + node);

				if (!cs_set.containsKey(node)) {
					// Util.log("skip");
					continue;
				} else {
					Set<Integer> callee_mids = cs_set.get(node);

					for (Iterator<Integer> it = callee_mids.iterator(); it.hasNext();) {
						int callee_mid = it.next();
						ret.add(new SearchNode(callee_mid, start));
						Util.log("add " + start.mid + "-->" + callee_mid);
					}
				}
			}
		}

		// Util.log(ret);

		return ret;
	}
}

class SearchNode {
	public int mid;
	public SearchNode parent;

	public SearchNode(int mid, SearchNode parent) {
		this.mid = mid;
		this.parent = parent;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		SearchNode other = (SearchNode) obj;
		if (mid != other.mid)
			return false;
		return true;
	}

	public String toString() {
		return "SearchNode (" + parent.mid + " --> " + mid + ")";
	}
}
