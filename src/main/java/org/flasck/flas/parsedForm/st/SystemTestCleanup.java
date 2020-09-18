package org.flasck.flas.parsedForm.st;

import org.flasck.flas.commonBase.names.SystemTestName;

public class SystemTestCleanup extends SystemTestStage {

	public SystemTestCleanup(SystemTestName name) {
		super(name, null);
	}
	
	@Override
	public String toString() {
		return "SystemTestFinally[" + name + "]";
	}
}
