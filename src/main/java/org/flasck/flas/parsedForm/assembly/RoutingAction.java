package org.flasck.flas.parsedForm.assembly;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.UnresolvedVar;

public class RoutingAction {
	public final String action;
	public final UnresolvedVar card;
	public final Expr expr;

	public RoutingAction(String action, UnresolvedVar card, Expr expr) {
		this.action = action;
		this.card = card;
		this.expr = expr;
	}
}
