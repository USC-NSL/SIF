package enl.sif.path;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import enl.sif.codepoint.Util;

public class ClassHierarchyMap {
	private static Map<String, List<String>> map = new HashMap<String, List<String>>(); // cname --> sup_cname ...
	private static Map<String, Integer> cname_id_map = new HashMap<String, Integer>(); // cname --> id
	private static Map<Integer, String> id_cname_map = new HashMap<Integer, String>(); // id --> cname
	private static Map<Integer, Set<Integer>> sub_map = new HashMap<Integer, Set<Integer>>(); // subclass relationship: id --> id ...

	public static void add(String cname, List<String> super_cnames) {
		if (!map.containsKey(cname)) {
			map.put(cname, super_cnames);
			Util.log(cname + " extends " + super_cnames);
		} else {
			Util.err("Already have " + cname);
		}
	}

	private static int add_cls_name(String cname) {
		int id;

		if (cname_id_map.containsKey(cname)) {
			id = cname_id_map.get(cname);
		} else {
			id = cname_id_map.size();
			cname_id_map.put(cname, id);
			id_cname_map.put(id, cname);
		}

		return id;
	}

	private static int get_cls_id(String cname) {
		return (cname_id_map.containsKey(cname)) ? cname_id_map.get(cname) : -1;
	}

	private static String get_cls_name(int id) {
		return (id_cname_map.containsKey(id)) ? id_cname_map.get(id) : null;
	}

	public static void buildMap() {
		for (Iterator<Entry<String, List<String>>> iter = map.entrySet().iterator(); iter.hasNext();) {
			Entry<String, List<String>> entry = iter.next();
			String cname = entry.getKey();
			List<String> sup_cnames = entry.getValue();

			int id = add_cls_name(cname);

			int _id;
			for (Iterator<String> _iter = sup_cnames.iterator(); _iter.hasNext();) {
				_id = add_cls_name(_iter.next());
				Set<Integer> ids = (sub_map.containsKey(_id)) ? sub_map.get(_id) : new HashSet<Integer>();
				ids.add(id);
				sub_map.put(_id, ids);
			}
		}
	}

	public static Set<String> getSubClassNames(String cname) {
		Set<String> ret = new HashSet<String>();
		int id = get_cls_id(cname);

		if (id >= 0) {
			Set<Integer> sub_ids = sub_map.get(id);

			if (sub_ids == null) {
				return ret;
			}

			for (Iterator<Integer> iter = sub_ids.iterator(); iter.hasNext();) {
				int sub_id = iter.next();
				String sub_cname = get_cls_name(sub_id);

				assert (sub_cname != null) : "id " + sub_id + " cannot be converted to class name";

				ret.add(sub_cname);
			}
		} else {
			// Util.err(cname + " cannot be converted to id");
		}

		return ret;
	}
}
