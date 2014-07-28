package enl.sif.path;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import enl.epp.Util;

public class ReachingMethodSolver {
	// private static final long serialVersionUID = 8065823618476342133L;
	private Map<Integer, Set<Integer>> graph;
	private Map<Integer, Set<Integer>> OUT; // store reachable callee nodes for each node
	private Set<Integer> nodes; // all nodes that may appear as caller or callee

	public ReachingMethodSolver(Map<Integer, Set<Integer>> graph) {
		this.graph = graph;
	}

	private void init() {
		nodes = new HashSet<Integer>();

		for (Iterator<Integer> iter = graph.keySet().iterator(); iter.hasNext();) {
			int node = iter.next();
			nodes.add(node);
			nodes.addAll(graph.get(node));
		}

		// Util.log("nodes: " + nodes);

		OUT = new HashMap<Integer, Set<Integer>>();

		for (Iterator<Integer> iter = nodes.iterator(); iter.hasNext();) {
			int node = iter.next();
			Set<Integer> callees = (graph.containsKey(node)) ? graph.get(node) : new HashSet<Integer>();
			OUT.put(node, callees);
		}
	}

	public Map<Integer, Set<Integer>> run() {
		init();

		boolean change = true;
		int cnt = 0;

		while (change) {
			change = false;
			cnt++;
			Util.log("iter=" + cnt);
			// Util.log(OUT);

			for (Iterator<Integer> iter = nodes.iterator(); iter.hasNext();) {
				int node = iter.next();

				if (!graph.containsKey(node))
					continue;

				Set<Integer> res = new HashSet<Integer>();
				Set<Integer> children = graph.get(node);

				for (Iterator<Integer> it = children.iterator(); it.hasNext();) {
					int child = it.next();
					res.addAll(OUT.get(child));
				}

				Set<Integer> out = OUT.get(node);
				if (out.addAll(res)) { // return true if out is changed
					change = true;
					OUT.put(node, out);
				}
			}
		}

		Util.log("cnt=" + cnt);

		return OUT;
	}

	public Set<Integer> getOut(int node) {
		return OUT.get(node);
	}

}
