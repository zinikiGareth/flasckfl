package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.ParenExpr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedOperator;

public class ParenTermConsumer implements ExprTermConsumer {
	public class ParenCloseRewriter implements ExprTermConsumer {
		private final Locatable from;
		private final String op;
		private Expr endToken;
		private int end;
		private final List<Expr> terms = new ArrayList<>();
		private StringLiteral currentVar;
		private Punctuator comma;

		public ParenCloseRewriter(Punctuator from, String op) {
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

		public void defineVar(StringLiteral sl) {
			// TODO: I think this can happen; write tests
			if (!(op.equals("{}")))
				throw new RuntimeException("Can't use colon here");
			currentVar = sl;
		}

		@Override
		public void term(Expr term) {
			if (op.equals("{}")) {
				if (currentVar == null)
					throw new RuntimeException("need field and colon"); // I don't think this can happen
				errors.logReduction("object-member", currentVar, term);
				term = new ApplyExpr(currentVar.location().copySetEnd(end), new UnresolvedOperator(from.location(), ":"), currentVar, term);
			}
			if (comma != null) {
				if (op.equals("()"))
					errors.logReduction("comma-expression", comma, term);
				else if (op.equals("[]"))
					errors.logReduction("comma-expression", comma, term);
				else if (op.equals("{}"))
					errors.logReduction("comma-object-member", comma, term);
				comma = null;
			}
			terms.add(term);
		}

		@Override
		public void done() {
			if (terms.size() == 0) {
				if (op.equals("()")) {
					errors.message(from.location(), "empty tuples are not permitted");
					return;
				} else if (op.equals("[]")) {
					errors.logReduction("empty-list-literal", from, endToken);
				} else {
					errors.logReduction("empty-object-literal", from, endToken);
				}
				builder.term(new ApplyExpr(from.location().copySetEnd(end), new UnresolvedOperator(from.location(), op)));
				return;
			}
			final Expr ae = terms.get(0);
			if (terms.size() == 1 && op.equals("()")) {
				errors.logReduction("paren-expression", from, endToken);
				builder.term(new ParenExpr(from.location().copySetEnd(end), ae));
			} else {
				if (op.equals("[]")) {
					errors.logReduction("non-empty-list-literal", from, endToken);
				} else if (op.equals("{}")) {
					errors.logReduction("non-empty-object-literal", from, endToken);
				} else
					errors.logReduction("tuple-expression", from, endToken);
				builder.term(new ApplyExpr(from.location().copySetEnd(end), new UnresolvedOperator(ae.location(), op), terms.toArray()));
			}
		}

		@Override
		public void showStack(StackDumper d) {
			throw new org.zinutils.exceptions.NotImplementedException();
		}

		public boolean isObjectLiteral() {
			return op.equals("{}");
		}

		public void commaAt(Punctuator comma) {
			this.comma = comma;
		}
	}

	private final ErrorReporter errors;
	private final ExprTermConsumer builder;
	private final Punctuator open;
	private final ParenCloseRewriter closer;
	private final TDAExprReducer curr;
	private boolean expectingColon;
	private Punctuator comma;

	public ParenTermConsumer(InputPosition from, ErrorReporter errors, ExprTermConsumer builder, Punctuator open) {
		this.errors = errors;
		this.builder = builder;
		this.open = open;
		if (open.is("("))
			closer = new ParenCloseRewriter(open, "()");
		else if (open.is("["))
			closer = new ParenCloseRewriter(open, "[]");
		else if (open.is("{")) {
			closer = new ParenCloseRewriter(open, "{}");
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
				curr.done();
			} else if (punc.is(",")) {
				comma = punc;
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
