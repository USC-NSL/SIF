package enl.sif.examples;

import org.apache.bcel.Constants;

import enl.sif.codepoint.BytecodePosition;
import enl.sif.codepoint.CP;
import enl.sif.codepoint.CPFinder;
import enl.sif.codepoint.CPInstrumenter;
import enl.sif.codepoint.InstructionArgType;
import enl.sif.codepoint.InstrumentOperation;
import enl.sif.codepoint.InstrumentPosition;
import enl.sif.codepoint.SIFARunnable;
import enl.sif.codepoint.ThisType;
import enl.sif.codepoint.UserDefinedInvoke;

public class FineGrainPerm implements SIFARunnable {
	public void run() {
		CPFinder.init();
		CPFinder.setClass("com.tweakersoft.aroundme.AroundMe", "android.app.Activity");
		CPFinder.setMethod("onCreate:\\(Landroid\\/os\\/Bundle;\\)V");
		CPFinder.setBytecode(BytecodePosition.ENTRY);

		for (CP cp : CPFinder.apply()) {
			UserDefinedInvoke code = new UserDefinedInvoke("enl.sif.examples.MyLogger", "interceptActivity", new ThisType());
			CPInstrumenter.dryRun(cp, InstrumentOperation.INSERT, InstrumentPosition.BEFORE, code);
		}

		CPFinder.init();
		CPFinder.setClass("com.flurry.*", null);
		CPFinder.setBytecode(Constants.INVOKEINTERFACE, ".*HttpClient.execute(.*HttpUriRequest.*)");

		for (CP cp : CPFinder.apply()) {
			UserDefinedInvoke code = new UserDefinedInvoke("enl.sif.examples.MyLogger", "checkPerm", InstructionArgType.ALL_ARGS);
			CPInstrumenter.dryRun(cp, InstrumentOperation.INSERT, InstrumentPosition.AT, code);
		}

		CPInstrumenter.exec();
	}
}
