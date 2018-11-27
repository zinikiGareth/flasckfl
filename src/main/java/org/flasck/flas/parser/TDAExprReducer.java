package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedOperator;

public class TDAExprReducer implements ExprTermConsumer {
	public static class OpPrec {
		int pos;
		int prec;

		public OpPrec(int pos, int prec) {
			this.pos = pos;
			this.prec = prec;
		}
	}

	private final ErrorReporter errors;
	private final ExprTermConsumer builder;
	private final List<Expr> terms = new ArrayList<>();
	private final List<OpPrec> ops = new ArrayList<>();

	public TDAExprReducer(ErrorReporter errors, ExprTermConsumer builder) {
		this.errors = errors;
		this.builder = builder;
	}

	@Override
	public void term(Expr term) {
		if (term instanceof UnresolvedOperator)
			ops.add(new OpPrec(terms.size(), precedence(term.location(), ((UnresolvedOperator)term).op)));
		this.terms.add(term);
	}

	@Override
	public void done() {
		if (terms.isEmpty())
			return;
		builder.term(reduce(0, terms.size()));
	}
	
	private Expr reduce(int from, int to) {
		OpPrec op = null;
		for (OpPrec p : ops)
			if (p.pos >= from && p.pos < to && (op == null || p.prec < op.prec))
				op = p;
		if (op != null)
			return handleOperators(from, to, op.pos);
		else
			return fncall(from, to);
	}

	private Expr handleOperators(int from, int to, int oppos) {
		Expr oe = terms.get(oppos);
		if (oppos == from) { // unary operator
			return new ApplyExpr(oe.location().copySetEnd(terms.get(to-1).location().pastEnd()), oe, reduce(from+1, to));
		} else {
			return new ApplyExpr(terms.get(0).location().copySetEnd(terms.get(terms.size()-1).location().pastEnd()), oe, reduce(from, oppos), reduce(oppos+1, to));
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

	private int precedence(InputPosition pos, String op) {
		boolean isUnary = terms.isEmpty() || (!ops.isEmpty() && ops.get(ops.size()-1).pos == terms.size()-1);
		switch (op) {
		case "*":
		case "/":
			return 6;
		case "-":
			if (isUnary)
				return 9;
		case "+":
			return 5;
		default:
			errors.message(pos, "there is no precedence for operator " + op);
			return 0;
		}
	}
}
