package enl.sif.examples;

import enl.sif.codepoint.BytecodePosition;
import enl.sif.codepoint.CP;
import enl.sif.codepoint.CPFinder;
import enl.sif.codepoint.CPInstrumenter;
import enl.sif.codepoint.InstrumentOperation;
import enl.sif.codepoint.InstrumentPosition;
import enl.sif.codepoint.SIFARunnable;
import enl.sif.codepoint.ThisType;
import enl.sif.codepoint.UserDefinedInvoke;

public class MethodTracing implements SIFARunnable {
	public void run() {
		CPFinder.init();
		CPFinder.setClass(null, "android.app.Activity");
		CPFinder.setMethod("onCreate:\\(Landroid\\/os\\/Bundle;\\)V");
		CPFinder.setBytecode(BytecodePosition.ENTRY);

		for (CP cp : CPFinder.apply()) {
			UserDefinedInvoke code = new UserDefinedInvoke("enl.sif.examples.MyLogger", "start", new ThisType());
			CPInstrumenter.dryRun(cp, InstrumentOperation.INSERT, InstrumentPosition.BEFORE, code);
		}

		CPInstrumenter.exec();
	}
}
