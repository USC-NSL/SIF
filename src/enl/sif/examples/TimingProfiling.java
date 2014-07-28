package enl.sif.examples;

import org.apache.bcel.Constants;

import enl.sif.codepoint.CP;
import enl.sif.codepoint.CPFinder;
import enl.sif.codepoint.CPInstrumenter;
import enl.sif.codepoint.CodePointType;
import enl.sif.codepoint.InstrumentOperation;
import enl.sif.codepoint.InstrumentPosition;
import enl.sif.codepoint.SIFARunnable;
import enl.sif.codepoint.UserDefinedInvoke;

public class TimingProfiling implements SIFARunnable {

	public void run() {
		CPFinder.init();
		CPFinder.setBytecode(Constants.INVOKEVIRTUAL, ".* native .*");

		for (CP cp : CPFinder.apply()) {
			System.out.println(cp);
			UserDefinedInvoke code = new UserDefinedInvoke("enl.sif.examples.MyLogger", "logEntry", new CodePointType());
			CPInstrumenter.dryRun(cp, InstrumentOperation.INSERT, InstrumentPosition.BEFORE, code);

			code = new UserDefinedInvoke("enl.sif.examples.MyLogger", "logExit", new CodePointType());
			CPInstrumenter.dryRun(cp, InstrumentOperation.INSERT, InstrumentPosition.AFTER, code);
		}

		CPInstrumenter.exec();
	}
}
