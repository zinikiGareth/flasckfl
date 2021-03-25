package org.flasck.flas.parsedForm.st;

import org.flasck.flas.commonBase.names.SystemTestName;
import org.flasck.flas.parser.ut.IntroductionConsumer;

public class SystemTestConfiguration extends SystemTestStage {
	public SystemTestConfiguration(SystemTestName name, IntroductionConsumer topLevel) {
		super(name, null, topLevel);
	}
	
	@Override
	public String toString() {
		return "SystemTestConfig[" + name + "]";
	}
}
