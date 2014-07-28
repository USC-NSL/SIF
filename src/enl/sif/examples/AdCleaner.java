package enl.sif.examples;

import org.apache.bcel.Constants;

import enl.sif.codepoint.CP;
import enl.sif.codepoint.CPFinder;
import enl.sif.codepoint.CPInstrumenter;
import enl.sif.codepoint.InstructionArgType;
import enl.sif.codepoint.InstrumentOperation;
import enl.sif.codepoint.InstrumentPosition;
import enl.sif.codepoint.SIFARunnable;
import enl.sif.codepoint.UserDefinedInvoke;

public class AdCleaner implements SIFARunnable {
	public void run() {
		CPFinder.init();
		CPFinder.setBytecode(Constants.INVOKEVIRTUAL, "com.google.ads.AdView.loadAd\\(com.google.ads.AdRequest\\)");

		for (CP cp : CPFinder.apply()) {
			UserDefinedInvoke code = new UserDefinedInvoke("enl.sif.examples.MyLogger", "loadAdStub", new InstructionArgType(0));
			CPInstrumenter.dryRun(cp, InstrumentOperation.UPDATE, InstrumentPosition.AT, code);
		}

		CPInstrumenter.exec();
	}
}
