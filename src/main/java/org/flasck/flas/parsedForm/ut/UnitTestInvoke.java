package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.commonBase.Expr;

public class UnitTestInvoke implements UnitTestStep {
	public final Expr expr;
	
	public UnitTestInvoke(Expr expr) {
		this.expr = expr;
	}
}
