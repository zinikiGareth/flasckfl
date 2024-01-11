package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.commonBase.Expr;

public class UnitTestIdentical implements UnitTestStep {
	public final Expr expr;
	public final Expr value;

	public UnitTestIdentical(Expr expr, Expr value) {
		this.expr = expr;
		this.value = value;
	}
}
