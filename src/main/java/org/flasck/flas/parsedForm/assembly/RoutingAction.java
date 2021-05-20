package org.flasck.flas.parsedForm.assembly;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.zinutils.exceptions.CantHappenException;

public class RoutingAction {
	public final String action;
	public final UnresolvedVar card;
	public final List<Expr> exprs;
	public final TypeReference contract;
	private final Map<Integer, Integer> exprRefs = new HashMap<>();

	public RoutingAction(UnresolvedVar card, TypeReference contract, String action, List<Expr> exprs) {
		this.action = action;
		this.contract = contract;
		this.card = card;
		this.exprs = exprs;
	}
	
	public void exprFn(int expr, int fn) {
		exprRefs.put(expr, fn);
	}
	
	public Integer exprFor(int expr) {
		if (!exprRefs.containsKey(expr))
			throw new CantHappenException("expr not found " + expr);
		return exprRefs.get(expr);
	}
}
