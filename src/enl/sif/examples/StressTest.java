package enl.sif.examples;

import org.apache.bcel.Constants;

import enl.epp.Util;
import enl.sif.codepoint.CP;
import enl.sif.codepoint.CPFinder;
import enl.sif.codepoint.CPInstrumenter;
import enl.sif.codepoint.InstructionArgType;
import enl.sif.codepoint.InstrumentOperation;
import enl.sif.codepoint.InstrumentPosition;
import enl.sif.codepoint.SIFARunnable;
import enl.sif.codepoint.UserDefinedInvoke;

public class StressTest implements SIFARunnable {
	public void run() {
		CPFinder.init();
		CPFinder.setBytecode(Constants.INVOKEINTERFACE, "org.apache.http.client.HttpClient.execute\\(org.apache.http.client.methods.HttpUriRequest\\)");

		for (CP cp : CPFinder.apply()) {
			Util.log(cp.toString());
			UserDefinedInvoke code = new UserDefinedInvoke("enl.sif.examples.MyHTTPClient", "execute", new InstructionArgType(0));
			CPInstrumenter.dryRun(cp, InstrumentOperation.UPDATE, InstrumentPosition.AT, code);
		}

		CPInstrumenter.exec();
	}
}