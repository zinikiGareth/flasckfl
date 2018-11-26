package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;

public class TDAExprReducer implements ExprTermConsumer {
	private final ExprTermConsumer builder;
	private final List<Expr> terms = new ArrayList<>();

	public TDAExprReducer(ErrorReporter errors, ExprTermConsumer builder) {
		this.builder = builder;
	}

	@Override
	public void term(Expr term) {
		this.terms.add(term);
	}

	@Override
	public void done() {
		if (terms.isEmpty())
			return;
		final Expr t0 = terms.remove(0);
		if (terms.size() == 0)
			builder.term(t0);
		else if (terms.size() > 0) 
			builder.term(new ApplyExpr(t0.location(), t0, terms.toArray()));
	}
}
