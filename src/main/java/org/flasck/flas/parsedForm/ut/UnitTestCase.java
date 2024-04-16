package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.commonBase.names.UnitTestName;

public class UnitTestCase extends TestStepHolder {
	public final UnitTestName name;
	public final String description;
	private boolean runJS = true;
	
	public UnitTestCase(UnitTestName name, String description) {
		this.name = name;
		this.description = description;
	}

	@Override
	public String toString() {
		return "UnitTestCase[" + description + "]";
	}

	public void dontRunJS() {
		this.runJS = false;
	}
	
	public boolean shouldRunJS() {
		return runJS;
	}
}
