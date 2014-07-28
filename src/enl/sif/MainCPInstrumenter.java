package enl.sif;

import enl.sif.codepoint.ClassFileScanner;
import enl.sif.codepoint.SIFARunnable;

public class MainCPInstrumenter {
	public static void main(String[] args) {
		if (args.length != 2) {
			// Util.err("");
			// System.exit(0);
		}

		ClassFileScanner cfs = new ClassFileScanner(Config.INPUT_DIR); // args[0]);
		cfs.scan();

		try {
			@SuppressWarnings("unchecked")
			// Class<SIFARunnable> cls = (Class<SIFARunnable>) ClassLoader.getSystemClassLoader().loadClass("enl.sif.examples.PLVS");
			// Class<SIFARunnable> cls = (Class<SIFARunnable>) ClassLoader.getSystemClassLoader().loadClass("enl.sif.examples.PermUsageProfiling");
			// Class<SIFARunnable> cls = (Class<SIFARunnable>) ClassLoader.getSystemClassLoader().loadClass("enl.sif.examples.TimingProfiling");
			// Class<SIFARunnable> cls = (Class<SIFARunnable>) ClassLoader.getSystemClassLoader().loadClass("enl.sif.examples.FineGrainPerm");
			// Class<SIFARunnable> cls = (Class<SIFARunnable>) ClassLoader.getSystemClassLoader().loadClass("enl.sif.examples.LocationAuditor");
			// Class<SIFARunnable> cls = (Class<SIFARunnable>) ClassLoader.getSystemClassLoader().loadClass("enl.sif.examples.AdCleaner");
			Class<SIFARunnable> cls = (Class<SIFARunnable>) ClassLoader.getSystemClassLoader().loadClass("enl.sif.examples.StressTest");
			// Class<SIFARunnable> cls = (Class<SIFARunnable>) ClassLoader.getSystemClassLoader().loadClass("enl.sif.examples.FlurryAnalytics");
			cls.newInstance().run();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
