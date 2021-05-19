package org.flasck.flas.parsedForm.assembly;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;

public class RoutingAction {
	public final String action;
	public final UnresolvedVar card;
	public final List<Expr> exprs;
	public final TypeReference contract;

	public RoutingAction(String action, UnresolvedVar card, List<Expr> exprs) {
		this.action = action;
		this.contract = new TypeReference(card.location(), "Lifecycle", new ArrayList<>());
		this.card = card;
		this.exprs = exprs;
	}
}
