package org.flasck.flas.parsedForm.st;

import org.flasck.flas.commonBase.names.SystemTestName;
import org.flasck.flas.parser.ut.IntroductionConsumer;

public class SystemTestCleanup extends SystemTestStage {

	public SystemTestCleanup(SystemTestName name, IntroductionConsumer topLevel) {
		super(name, null, topLevel);
	}
	
	@Override
	public String toString() {
		return "SystemTestFinally[" + name + "]";
	}
}
