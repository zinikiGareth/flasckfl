package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.DotOperator;
import org.flasck.flas.parsedForm.TypeExpr;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.ParenTermConsumer.ParenCloseRewriter;
import org.zinutils.exceptions.NotImplementedException;

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
	private DotOperator haveDot;
	private boolean haveErrors;

	public TDAExprReducer(ErrorReporter errors, ExprTermConsumer builder) {
		this.errors = errors;
		this.builder = builder;
	}

	@Override
	public void term(Expr term) {
		if (haveErrors)
			return;
		if (haveDot != null) {
			if (!(term instanceof UnresolvedVar)) {
				errors.message(term.location(), "field access requires a field name");
				haveErrors = true;
				return;
			} else {
				Expr strobj = terms.remove(terms.size() - 1);
				terms.add(new MemberExpr(strobj.location().copySetEnd(term.location().pastEnd()), strobj, term));
				haveDot = null;
				return;
			}
		}
		if (term instanceof Punctuator) {
			if (((Punctuator) term).is(":")) {
				term = new UnresolvedOperator(term.location(), ":");
			} else {
				errors.message(term.location(), "invalid tokens after expression");
				haveErrors = true;
				return;
			}
		}
		if (term instanceof DotOperator) {
			if (terms.isEmpty()) {
				errors.message(term.location(), "field access requires a struct or object");
				haveErrors = true;
				return;
			}
			haveDot = (DotOperator) term;
			return;
		}
		if (term instanceof UnresolvedOperator) {
			final UnresolvedOperator op = (UnresolvedOperator)term;
			ops.add(new OpPrec(terms.size(), precedence(term.location(), op.op)));
		}
		this.terms.add(term);
	}

	public void seenComma() {
		builder.term(reduce(0, terms.size()));
		terms.clear();
		ops.clear();
	}

	public void seenColon(ParenCloseRewriter closer) {
		closer.defineVar(((UnresolvedVar)terms.get(0)).var);
		terms.clear();
	}

	@Override
	public void done() {
		if (!haveErrors && haveDot != null) {
			errors.message(haveDot.location(), "field access requires an explicit field name");
			haveErrors = true;
		}
		if (!haveErrors && !terms.isEmpty()) {
			Expr r = reduce(0, terms.size());
			if (r != null)
				builder.term(r);
		}
		builder.done();
	}
	
	private Expr reduce(int from, int to) {
		OpPrec op = null;
		for (OpPrec p : ops)
			if (p.pos >= from && p.pos < to && (op == null || p.prec < op.prec))
				op = p;
		if (op != null)
			return handleOperators(from, to, op.pos, op.prec);
		else
			return fncall(from, to);
	}

	private Expr handleOperators(int from, int to, int oppos, int prec) {
		Expr oe = terms.get(oppos);
		if (oppos == from) { // unary operator
			return new ApplyExpr(oe.location().copySetEnd(terms.get(to-1).location().pastEnd()), oe, reduce(from+1, to));
		} else if (prec == 10) {
			throw new NotImplementedException();
		} else {
			final Expr rhs = reduce(oppos+1, to);
			final Expr lhs = reduce(from, oppos);
			return new ApplyExpr(terms.get(from).location().copySetEnd(terms.get(terms.size()-1).location().pastEnd()), oe, lhs, rhs);
		}
	}

	private Expr fncall(int from, int to) {
		final Expr t0 = terms.get(from);
		if (isTypeExpr(t0))
			return resolveTypeExpr(t0, from, to);
		if (from+1 == to && !isConstructor(t0))
			return t0;
		else 
			return new ApplyExpr(t0.location().copySetEnd(terms.get(terms.size()-1).location().pastEnd()), t0, args(from+1, to).toArray());
	}

	private boolean isConstructor(Expr t0) {
		return t0 instanceof UnresolvedVar && ((UnresolvedVar)t0).isConstructor();
	}
	
	private boolean isTypeExpr(Expr t0) {
		return t0 instanceof UnresolvedVar && ((UnresolvedVar)t0).isType();
	}

	private Expr resolveTypeExpr(Expr t0, int from, int to) {
		if (to != from+2) {
			errors.message(t0.location(), "type operator must have exactly one argument");
			return null;
		}
		Expr ctor = terms.get(from+1);
		return new TypeExpr(t0.location().copySetEnd(ctor.location().pastEnd()), ctor.location(), ((UnresolvedVar)ctor).var);
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
		case "++":
			return 5;
		case ":":
			return 3;
		case "==":
		case "<":
		case "<=":
		case "!=":
		case ">":
		case ">=":
			return 2;
		default:
			errors.message(pos, "there is no precedence for operator " + op);
			return 0;
		}
	}

	@Override
	public void showStack(StackDumper d) {
		d.dump(terms);
	}
}
