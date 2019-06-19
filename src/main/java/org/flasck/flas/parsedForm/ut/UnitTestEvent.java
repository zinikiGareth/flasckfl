package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.parsedForm.UnresolvedVar;

public class UnitTestEvent implements UnitTestStep {
	public final UnresolvedVar card;
	public final StringLiteral field;
	public final Expr expr;
	
	public UnitTestEvent(UnresolvedVar card, StringLiteral field, Expr expr) {
		this.card = card;
		this.field = field;
		this.expr = expr;
	}
}
