package org.flasck.flas.testrunner;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.parsedForm.IScope;
import org.flasck.flas.parsedForm.Scope;

public class UnitTests implements Locatable {
	private final Scope scope;

	public UnitTests(IScope defineIn, String asName) {
		final PackageName pkg = new PackageName(asName);
		defineIn.define(pkg.finalPart(), this);
		scope = new Scope(pkg);
	}

	public Scope scope() {
		return scope;
	}

	@Override
	public InputPosition location() {
		return new InputPosition("unitTests", 0, 0, "unit test definitions");
	}

}
