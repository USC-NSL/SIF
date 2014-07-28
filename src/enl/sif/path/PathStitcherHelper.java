package enl.sif.path;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import enl.sif.codepoint.Util;

public class PathStitcherHelper {
	public static List<LogRecord> load(String log_fn) {
		List<LogRecord> ret = new ArrayList<LogRecord>();
		LogRecord entry = null;

		try {
			BufferedReader br = new BufferedReader(new FileReader(log_fn));
			String line;

			while ((line = br.readLine()) != null) {
				String[] arr = line.split(" ");
				String type = arr[0];
				int mid = Integer.parseInt(arr[1]);
				short tid = Short.parseShort(arr[arr.length - 1]);

				if (type.equals("ENTRY")) {
					entry = new EntryExitLogRecord(EntryExitLogRecord.ENTRY, mid, tid);
				} else if (type.equals("EXIT")) {
					entry = new EntryExitLogRecord(EntryExitLogRecord.EXIT, mid, tid);
				} else if (type.equals("PS")) {
					long pid = Long.parseLong(arr[arr.length - 2]);
					short fid = Short.parseShort(arr[arr.length - 3]);
					entry = new PathSegmentLogRecord(fid, pid, tid);
				} else if (type.equals("CS")) {
					int cs = Integer.parseInt(arr[1]);
					entry = new CallSiteLogRecord(cs, tid);
				} else { // UNKNOWN type
					entry = null;
				}

				assert (entry != null) : "LogRecord is NULL";

				// remove last CS if the next one is CS too ==> only the adjacent CS will be used for next ENTRY record
				if (!ret.isEmpty()) {
					LogRecord lr = ret.get(ret.size() - 1);
					if ((lr instanceof CallSiteLogRecord) && (entry instanceof CallSiteLogRecord)) {
						ret.remove(ret.size() - 1);
					}
				}
				ret.add(entry);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (int i = 0; i < ret.size(); i++) {
			Util.log(ret.get(i).toString());
		}

		return ret;
	}
}
