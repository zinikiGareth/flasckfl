package org.flasck.flas.parsedForm.st;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.parsedForm.ut.UnitTestStep;

public class CreateMockApplication implements UnitTestStep {
	private final MockApplication ma;

	public CreateMockApplication(MockApplication ma) {
		this.ma = ma;
	}

	public String name() {
		return this.ma.asVar();
	}

	public PackageName pkg() {
		return this.ma.pkgName;
	}
}
