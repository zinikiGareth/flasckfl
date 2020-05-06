package org.flasck.flas.parsedForm.ut;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parsedForm.UnresolvedVar;

public class UnitTestEvent implements UnitTestStep {
	public final UnresolvedVar card;
	public final TargetZone targetZone;
	public final Expr expr;
	
	public UnitTestEvent(UnresolvedVar card, TargetZone targetZone, Expr expr) {
		this.card = card;
		this.targetZone = targetZone;
		this.expr = expr;
	}
}
