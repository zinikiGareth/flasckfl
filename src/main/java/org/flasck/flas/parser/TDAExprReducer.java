package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedOperator;

public class TDAExprReducer implements ExprTermConsumer {
	private final ExprTermConsumer builder;
	private final List<Expr> terms = new ArrayList<>();
	private final List<Integer> ops = new ArrayList<>();

	public TDAExprReducer(ErrorReporter errors, ExprTermConsumer builder) {
		this.builder = builder;
	}

	@Override
	public void term(Expr term) {
		if (term instanceof UnresolvedOperator)
			ops.add(terms.size());
		this.terms.add(term);
	}

	@Override
	public void done() {
		if (terms.isEmpty())
			return;
		builder.term(reduce(0, terms.size()));
	}
	
	private Expr reduce(int from, int to) {
		int pos = -1;
		for (int p : ops)
			if (p >= from && p < to)
				pos = p;
		if (pos != -1)
			return handleOperators(from, to, pos);
		else
			return fncall(from, to);
	}

	private Expr handleOperators(int from, int to, int oppos) {
		Expr oe = terms.get(oppos);
		if (oppos == from) { // unary operator
			return new ApplyExpr(oe.location().copySetEnd(terms.get(to-1).location().pastEnd()), oe, args(from+1, to).toArray());
		} else {
			return new ApplyExpr(terms.get(0).location().copySetEnd(terms.get(terms.size()-1).location().pastEnd()), oe, fncall(from, oppos), fncall(oppos+1, to));
		}
	}

	private Expr fncall(int from, int to) {
		final Expr t0 = terms.get(from);
		if (from+1 == to)
			return t0;
		else 
			return new ApplyExpr(t0.location().copySetEnd(terms.get(terms.size()-1).location().pastEnd()), t0, args(from+1, to).toArray());
	}

	private List<Expr> args(int from, int to) {
		List<Expr> ret = new ArrayList<>();
		for (int i=from;i<to;i++)
			ret.add(terms.get(i));
		return ret;
	}
}
