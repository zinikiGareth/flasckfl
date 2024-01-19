package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.MemberExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.CheckTypeExpr;
import org.flasck.flas.parsedForm.DotOperator;
import org.flasck.flas.parsedForm.TypeExpr;
import org.flasck.flas.parsedForm.TypeReference;
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
	private final ErrorMark mark;
	private final ExprTermConsumer builder;
	private final List<Expr> terms = new ArrayList<>();
	private final List<OpPrec> ops = new ArrayList<>();
	private DotOperator haveDot;
	private boolean haveErrors;
	private boolean reduceToOne;

	public TDAExprReducer(ErrorReporter errors, ExprTermConsumer builder, boolean reduceToOne) {
		this.errors = errors;
		this.mark = errors.mark();
		this.builder = builder;
		this.reduceToOne = reduceToOne;
	}

	@Override
	public boolean isTop() {
		return false;
	}

	@Override
	public void term(Expr term) {
		if (haveErrors)
			return;
		if (haveDot != null) {
			if (!(term instanceof UnresolvedVar) && !(term instanceof TypeReference)) {
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
		Expr t = terms.get(0);
		if (t instanceof UnresolvedVar)
			closer.defineVar(((UnresolvedVar)t).var);
		else if (t instanceof StringLiteral)
			closer.defineVar(((StringLiteral)t).text);
		terms.clear();
	}

	@Override
	public void done() {
		if (!haveErrors && haveDot != null) {
			errors.message(haveDot.location(), "field access requires an explicit field name");
			haveErrors = true;
		}
		if (!haveErrors && !terms.isEmpty()) {
			if (reduceToOne) {
				Expr r = reduce(0, terms.size());
				if (r != null)
					builder.term(r);
			} else {
				for (Expr t : terms)
					builder.term(t);
			}
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
			errors.logReduction("expr_binop", lhs, rhs);
			return new ApplyExpr(terms.get(from).location().copySetEnd(terms.get(terms.size()-1).location().pastEnd()), oe, lhs, rhs);
		}
	}

	private Expr fncall(int from, int to) {
		final Expr t0 = terms.get(from);
		if (isCastExpr(t0))
			return resolveCastExpr(t0, from, to);
		if (isTypeExpr(t0))
			return resolveTypeExpr(t0, from, to);
		if (isCheckTypeExpr(t0))
			return resolveCheckTypeExpr(t0, from, to);
		if (from+1 == to && !isConstructor(t0))
			return t0;
		else 
			return new ApplyExpr(t0.location().copySetEnd(terms.get(to-1).location().pastEnd()), t0, args(from+1, to).toArray());
	}

	private boolean isConstructor(Expr t0) {
		return t0 instanceof TypeReference;
	}
	
	private boolean isCastExpr(Expr t0) {
		return t0 instanceof UnresolvedVar && ((UnresolvedVar)t0).isCast();
	}

	private boolean isTypeExpr(Expr t0) {
		return t0 instanceof UnresolvedVar && ((UnresolvedVar)t0).isType();
	}

	private boolean isCheckTypeExpr(Expr t0) {
		return t0 instanceof UnresolvedVar && ((UnresolvedVar)t0).isCheckType();
	}

	private Expr resolveCastExpr(Expr t0, int from, int to) {
		if (mark.hasMoreNow())
			return null;
		if (to != from+3) {
			errors.message(t0.location(), "cast must have exactly two arguments");
			return null;
		}
		Expr type = terms.get(from+1);
		Expr val = terms.get(from+2);
		TypeReference tr;
		if (type instanceof TypeReference)
			tr = (TypeReference) type;
		else if (type instanceof MemberExpr) {
			MemberExpr me = (MemberExpr) type;
			tr = resolveMemberExprToTypeReference(me);
			if (tr == null)
				return null;
		} else {
			errors.message(type.location(), "syntax error in cast");
			return null;
		}
		return new CastExpr(t0.location().copySetEnd(type.location().pastEnd()), type.location(), val.location(), tr, val);
	}

	private TypeReference resolveMemberExprToTypeReference(MemberExpr me) {
		String prefix;
		if (me.from instanceof MemberExpr) {
			TypeReference inner = resolveMemberExprToTypeReference((MemberExpr) me.from);
			if (inner == null)
				return null;
			prefix = inner.name();
		} else if (me.from instanceof UnresolvedVar) {
			prefix = ((UnresolvedVar)me.from).var;
		} else
			return null;
		return new TypeReference(me.location(), prefix + "." + me.fld);
	}

	private Expr resolveTypeExpr(Expr t0, int from, int to) {
		if (to != from+2) {
			errors.message(t0.location(), "type must have exactly one argument");
			return null;
		}
		Expr ctor = terms.get(from+1);
		return new TypeExpr(t0.location().copySetEnd(ctor.location().pastEnd()), ctor.location(), ctor);
	}
	
	private Expr resolveCheckTypeExpr(Expr t0, int from, int to) {
		// TODO: I have forgotten how this reduction works; obviously the argument can be an expression
		// we really just want to check that the first thing is a type
		if (to != from+3) {
			errors.message(t0.location(), "istype must have exactly two arguments");
			return null;
		}
		Expr ctor = terms.get(from+1);
		return new CheckTypeExpr(t0.location().copySetEnd(ctor.location().pastEnd()), ctor.location(), ctor, terms.get(from+2));
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
		case "!":
			return 7;
		case "-":
			return isUnary ? 7 : 5;
		case "*":
		case "/":
		case "%":
			return 6;
		case "+":
		case "++":
			return 5;
		case ":":
			return 4;
		case "==":
		case "<>":
		case "<":
		case "<=":
		case "!=":
		case ">":
		case ">=":
			return 3;
		case "&&":
		case "||":
			return 2;
		case "->":
			return 1;
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
