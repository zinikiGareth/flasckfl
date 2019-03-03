package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedOperator;

public class ParenTermConsumer implements ExprTermConsumer {
	public class ParenCloseRewriter implements ExprTermConsumer {
		private final InputPosition from;
		private final String op;
		private int end;
		private final List<Expr> terms = new ArrayList<>();
		private String currentVar;

		public ParenCloseRewriter(InputPosition from, String op) {
			this.from = from;
			this.op = op;
		}
		
		public void endAt(Expr term) {
			end = term.location().pastEnd();
		}

		public void defineVar(String var) {
			// TODO: I think this can happen; write tests
			if (!(op.equals("{}")))
				throw new RuntimeException("Can't use colon here");
			currentVar = var;
		}

		@Override
		public void term(Expr term) {
			if (term instanceof ApplyExpr) {
				ApplyExpr ae = (ApplyExpr) term;
				term = new ApplyExpr(from.copySetEnd(end), ae.fn, ae.args);
			}
			if (op.equals("{}")) {
				if (currentVar == null)
					throw new RuntimeException("need field and colon"); // I don't think this can happen
				term = new ApplyExpr(from.copySetEnd(end), new UnresolvedOperator(from, ":"), new StringLiteral(from, currentVar), term);
			}
			terms.add(term);
		}

		@Override
		public void done() {
			if (terms.size() == 0) {
				if (op.equals("()"))
					errors.message(from, "empty tuples are not permitted");
				else
					builder.term(new ApplyExpr(from.copySetEnd(end), new UnresolvedOperator(from, op)));
				return;
			}
			final Expr ae = terms.get(0);
			if (terms.size() == 1 && op.equals("()"))
				builder.term(ae);
			else
				builder.term(new ApplyExpr(ae.location().copySetEnd(end), new UnresolvedOperator(ae.location(), op), terms.toArray()));
		}

		@Override
		public void showStack(StackDumper d) {
			throw new org.zinutils.exceptions.NotImplementedException();
		}
	}

	private final ErrorReporter errors;
	private final ExprTermConsumer builder;
	private final ParenCloseRewriter closer;
	private final TDAExprReducer curr;

	public ParenTermConsumer(InputPosition from, ErrorReporter errors, ExprTermConsumer builder, Punctuator open) {
		this.errors = errors;
		this.builder = builder;
		if (open.is("("))
			closer = new ParenCloseRewriter(from, "()");
		else if (open.is("["))
			closer = new ParenCloseRewriter(from, "[]");
		else if (open.is("{"))
			closer = new ParenCloseRewriter(from, "{}");
		else
			throw new RuntimeException("invalid open paren");
		curr = new TDAExprReducer(errors, closer);
	}
	
	@Override
	public void term(Expr term) {
		if (term instanceof Punctuator) {
			Punctuator punc = (Punctuator) term;
			if (punc.is(")") || punc.is("]") || punc.is("}")) {
				closer.endAt(term);
				curr.done();
			} else if (punc.is(",")) {
				curr.seenComma();
			} else if (punc.is(":")) {
				curr.seenColon(closer);
			} else if (punc.is("(")) {
				curr.term(term);
			} else
				throw new RuntimeException("Unexpected punc: " + punc);
		} else
			curr.term(term);
	}

	@Override
	public void done() {
		throw new RuntimeException("I don't think we should get here");
	}

	@Override
	public void showStack(StackDumper d) {
		curr.showStack(d);
	}

}
