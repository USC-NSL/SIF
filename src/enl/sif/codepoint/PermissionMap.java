package enl.sif.codepoint;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PermissionMap {
	// private static Map<String, Integer> api_map;
	private static Map<String, Set<String>> perm_map;

	// private static BitMatrix api_perm_map;

	public static void old_load(AndroidVersion version) {
		String file = "/home/haos/workspace/SIFA/lib/PScout/results/";
		switch (version) {
		case FROYO:
			file += "froyo_allmappings";
			break;
		case GINGERBREAD:
			file += "gingerbread_allmappings";
			break;
		case HONEYCOMB:
			file += "honeycomb_allmappings";
			break;
		case ICS:
			file += "ics_allmappings";
			break;
		case JELLYBEAN:
			file += "jellybean_allmappings";
			break;
		}

		// TODO: load and parse the file to 3 maps
		Util.log(file);

		// api_perm_map = new BitMatrix(api_map.size(), perm_map.size());
	}

	// for INTERNET permission only
	public static void load(AndroidVersion version) {

		perm_map = new HashMap<String, Set<String>>();
		// perm_map.put("android.permission.INTERNET", 0);

		String file;
		Set<String> api_set;

		file = "/home/haos/workspace/SIFA/lib/stowaway/permissionmap/haos_perm_internet.dat";
		api_set = new HashSet<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while ((line = br.readLine()) != null) {
				if (!api_set.contains(line))
					api_set.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Util.log("|api|=" + api_set.size());

		perm_map.put("android.permission.INTERNET", api_set);

		file = "/home/haos/workspace/SIFA/lib/PScout/results/haos_perm_location.dat";
		api_set = new HashSet<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;

			while ((line = br.readLine()) != null) {
				if (!api_set.contains(line))
					api_set.add(line);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Util.log("|api|=" + api_set.size());

		perm_map.put("LOCATION", api_set);
	}

	public static boolean checkPermUse(String api, String perm) {
		// assert (api_map.containsKey(api)) : "";
		assert (perm_map.containsKey(perm)) : perm + " NOT SUPPORTED YET";

		Set<String> api_set = perm_map.get(perm);
		return api_set.contains(api);

		// int api_idx = api_map.get(api);
		// int perm_idx = perm_map.get(perm);
		// return api_perm_map.get(api_idx, perm_idx);
	}
}
