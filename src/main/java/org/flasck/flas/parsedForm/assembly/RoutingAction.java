package org.flasck.flas.parsedForm.assembly;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;

public class RoutingAction {
	public final String action;
	public final UnresolvedVar card;
	public final List<Expr> exprs;
	public final TypeReference contract;

	public RoutingAction(UnresolvedVar card, TypeReference contract, String action, List<Expr> exprs) {
		this.action = action;
		this.contract = contract;
		this.card = card;
		this.exprs = exprs;
	}
}
