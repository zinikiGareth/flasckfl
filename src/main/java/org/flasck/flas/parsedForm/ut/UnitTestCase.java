package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parser.ut.UnitTestStepConsumer;

public class UnitTestCase implements UnitTestStepConsumer {
	public final String description;

	public UnitTestCase(String description) {
		this.description = description;
	}

	@Override
	public void assertion(Expr expr, Expr value) {
	}
}
