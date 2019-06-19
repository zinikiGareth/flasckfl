package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.commonBase.Expr;

public class UnitTestAssert implements UnitTestStep {
	public final Expr expr;
	public final Expr value;

	public UnitTestAssert(Expr expr, Expr value) {
		this.expr = expr;
		this.value = value;
	}
}
