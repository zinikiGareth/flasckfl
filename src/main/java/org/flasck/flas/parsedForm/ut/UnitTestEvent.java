package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.UnresolvedVar;

public class UnitTestEvent implements UnitTestStep {
	public final UnresolvedVar card;
	public final Expr expr;
	
	public UnitTestEvent(UnresolvedVar card, Expr expr) {
		this.card = card;
		this.expr = expr;
	}
}
