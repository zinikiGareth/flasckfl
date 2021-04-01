package org.flasck.flas.parsedForm.st;

import org.flasck.flas.parsedForm.ut.UnitTestStep;

public class CreateMockApplication implements UnitTestStep {
	private final MockApplication vn;

	public CreateMockApplication(MockApplication vn) {
		this.vn = vn;
	}

	public String name() {
		return this.vn.asVar();
	}
}
