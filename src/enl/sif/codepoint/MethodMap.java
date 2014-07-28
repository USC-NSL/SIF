package enl.sif.codepoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * This class stores the mapping between method signature and method id used in
 * path profiling.
 * 
 * @author haos@enl.usc.edu
 * 
 */
public class MethodMap {
	private static Map<MethodReference, Integer> map;
	private static Map<Integer, MethodReference> map2;
	private static int cnt;
	private static boolean init_done = false;

	private static void init() {
		map = new HashMap<MethodReference, Integer>();
		map2 = new HashMap<Integer, MethodReference>();
		cnt = 0;
		init_done = true;
	}

	/**
	 * Add method signature to map and increment the counter.
	 * 
	 * @param msig
	 * @return mid
	 */
	public static int addMethodSig(String cname, String mthdsig) {
		if (!init_done) {
			init();
		}

		int mid = cnt;

		MethodReference mref = new MethodReference(cname, mthdsig);
		if (map.containsKey(mref)) {
			mid = map.get(mref);
		} else {
			map.put(mref, mid);
			cnt++;
		}

		map2.put(mid, mref);

		return mid;
	}

	public static int getMethodID(String cname, String mthdsig) {
		int mid = -1;

		if (init_done && !map.isEmpty()) {
			MethodReference mref = new MethodReference(cname, mthdsig);

			if (map.containsKey(mref)) {
				mid = map.get(mref);
			} else {
				Util.log("map does not have " + mref);
			}
		}

		return mid;
	}

	/**
	 * Returns the class name of given method id.
	 * 
	 * @param mid
	 * @return class name as String
	 */
	public static String getClassName(int mid) {
		if (!init_done || map2.isEmpty() || !map2.containsKey(mid)) {
			return null;
		}

		return map2.get(mid).class_name;
	}

	public static String getMethodSig(int mid) {
		if (!init_done || map2.isEmpty() || !map2.containsKey(mid)) {
			return null;
		}

		return map2.get(mid).mthd_sig;
	}

	/**
	 * Save mappings to out_fn.
	 * 
	 * @param out_fn
	 */
	public static void save(String out_fn) {
		if (!init_done || map.isEmpty())
			return;
		PrintWriter pw = null;
		try {
			File file = new File(out_fn);
			File ofp = file.getParentFile();
			if (!ofp.exists()) {
				ofp.mkdirs();
			}

			pw = new PrintWriter(file);
			Iterator<Map.Entry<MethodReference, Integer>> iter = map.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<MethodReference, Integer> m_entry = iter.next();
				pw.println(m_entry.getValue() + "," + m_entry.getKey());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		finally {
			if (pw != null) {
				pw.flush();
				pw.close();
			}
		}
	}

	/**
	 * Load mapping from path_fn.
	 * 
	 * @param path_fn
	 */
	public static void load(String path_fn) {
		try {
			init();
			BufferedReader br = new BufferedReader(new FileReader(path_fn));
			String line;
			while ((line = br.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, ",");
				int mid = Integer.parseInt(st.nextToken());
				String cname = st.nextToken();
				String mthdsig = st.nextToken();
				MethodReference mref = new MethodReference(cname, mthdsig);
				map.put(mref, mid);
				map2.put(mid, mref);
			}
		} catch (Exception e) {
			Util.err("MethodMap: load() failed for " + path_fn);
			e.printStackTrace();
		}

		Util.log("Loaded " + map.size() + " methods.");
	}

	public static void dump() {
		Iterator<Map.Entry<MethodReference, Integer>> iter = map.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<MethodReference, Integer> m_entry = iter.next();
			Util.log(m_entry.getValue() + "," + m_entry.getKey());
		}
	}
}
