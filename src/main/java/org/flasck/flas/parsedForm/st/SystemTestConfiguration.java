package org.flasck.flas.parsedForm.st;

import org.flasck.flas.commonBase.names.SystemTestName;

public class SystemTestConfiguration extends SystemTestStage {
	public SystemTestConfiguration(SystemTestName name) {
		super(name, null);
	}
	
	@Override
	public String toString() {
		return "SystemTestConfig[" + name + "]";
	}
}
