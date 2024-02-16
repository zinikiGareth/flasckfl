package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.ParenExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.parsedForm.TypeExpr;
import org.flasck.flas.parsedForm.UnresolvedOperator;

public class ParenTermConsumer implements ExprTermConsumer {
	public class ParenCloseRewriter implements ExprTermConsumer {
		private final InputPosition from;
		private final String op;
		private Expr endToken;
		private int end;
		private final List<Expr> terms = new ArrayList<>();
		private String currentVar;
		private InputPosition comma;

		public ParenCloseRewriter(InputPosition from, String op) {
			this.from = from;
			this.op = op;
		}

		@Override
		public boolean isTop() {
			return false;
		}
		
		public void endAt(Expr term) {
			endToken = term;
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
			InputPosition loc = term.location();
			if (op.equals("{}")) {
				if (currentVar == null)
					throw new RuntimeException("need field and colon"); // I don't think this can happen
				term = new ApplyExpr(from.copySetEnd(end), new UnresolvedOperator(from, ":"), new StringLiteral(from, currentVar), term);
			}
			if (comma != null) {
				errors.logReduction("comma-expression", comma, loc);
				comma = null;
			}
			terms.add(term);
		}

		@Override
		public void done() {
			if (terms.size() == 0) {
				if (op.equals("()")) {
					errors.message(from, "empty tuples are not permitted");
					return;
				} else if (op.equals("[]")) {
					errors.logReduction("empty-list-literal", from, endToken.location());
				} else {
					errors.logReduction("empty-object-literal", from, endToken.location());
				}
				builder.term(new ApplyExpr(from.copySetEnd(end), new UnresolvedOperator(from, op)));
				return;
			}
			final Expr ae = terms.get(0);
			if (terms.size() == 1 && op.equals("()")) {
				errors.logReduction("paren-expression", from, endToken.location());
				builder.term(new ParenExpr(from, ae));
			} else {
				if (op.equals("[]")) {
					errors.logReduction("non-empty-list-literal", from, endToken.location());
				} else if (op.equals("{}")) {
					errors.logReduction("non-empty-object-literal", from, endToken.location());
				} else
					errors.logReduction("tuple-expression", from, endToken.location());
				builder.term(new ApplyExpr(from.copySetEnd(end), new UnresolvedOperator(ae.location(), op), terms.toArray()));
			}
		}

		@Override
		public void showStack(StackDumper d) {
			throw new org.zinutils.exceptions.NotImplementedException();
		}

		public boolean isObjectLiteral() {
			return op.equals("{}");
		}

		public void commaAt(InputPosition loc) {
			this.comma = loc;
		}
	}

	private final ErrorReporter errors;
	private final ExprTermConsumer builder;
	private final Punctuator open;
	private final ParenCloseRewriter closer;
	private final TDAExprReducer curr;
	private boolean expectingColon;
	private InputPosition comma;
	private InputPosition lastLoc;

	public ParenTermConsumer(InputPosition from, ErrorReporter errors, ExprTermConsumer builder, Punctuator open) {
		this.errors = errors;
		this.builder = builder;
		this.open = open;
		if (open.is("("))
			closer = new ParenCloseRewriter(open.location, "()");
		else if (open.is("["))
			closer = new ParenCloseRewriter(open.location, "[]");
		else if (open.is("{")) {
			closer = new ParenCloseRewriter(open.location, "{}");
			expectingColon = true;
		} else
			throw new RuntimeException("invalid open paren");
		curr = new TDAExprReducer(errors, closer, true);
	}
	
	@Override
	public boolean isTop() {
		return false;
	}

	@Override
	public void term(Expr term) {
		if (term instanceof Punctuator) {
			Punctuator punc = (Punctuator) term;
			if (punc.is(")") || punc.is("]") || punc.is("}")) {
				punc.checkCloserFor(errors, open);
				closer.endAt(term);
//				if (comma != null) { // we have a previous comma expression)
//					errors.logReduction("comma-expression", comma, lastLoc);
//				}
				curr.done();
			} else if (punc.is(",")) {
//				if (comma != null) { // we have a previous comma expression)
//					errors.logReduction("comma-expression", comma, lastLoc);
//				}
				comma = punc.location;
				curr.seenComma(comma);
				if (closer.isObjectLiteral())
					expectingColon = true;
			} else if (punc.is(":")) {
				if (closer.isObjectLiteral() && expectingColon && punc.is(":")) {
					curr.seenColon(closer);
					expectingColon = false;
				} else
					curr.term(new UnresolvedOperator(punc.location, ":"));
			} else if (punc.is("(")) {
				curr.term(term);
			} else
				throw new RuntimeException("Unexpected punc: " + punc);
		} else {
			curr.term(term);
		}
		lastLoc = term.location();
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
