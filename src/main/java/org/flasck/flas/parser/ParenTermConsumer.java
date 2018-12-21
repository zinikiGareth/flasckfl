package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.ApplyExpr;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;

public class ParenTermConsumer implements ExprTermConsumer {
	public class ParenCloseRewriter implements ExprTermConsumer {
		private final InputPosition from;
		private int end;

		public ParenCloseRewriter(InputPosition from) {
			this.from = from;
		}
		
		public void endAt(Expr term) {
			end = term.location().pastEnd();
		}

		@Override
		public void term(Expr term) {
			if (term instanceof ApplyExpr) {
				ApplyExpr ae = (ApplyExpr) term;
				term = new ApplyExpr(from.copySetEnd(end), ae.fn, ae.args);
			}
			builder.term(term);
		}

		@Override
		public void done() {
			throw new org.zinutils.exceptions.NotImplementedException();
		}
	}

//	private final ErrorReporter errors;
	private final ExprTermConsumer builder;
	private final Punctuator open;
	private final ParenCloseRewriter closer;
	private final TDAExprReducer curr;

	public ParenTermConsumer(InputPosition from, ErrorReporter errors, ExprTermConsumer builder, Punctuator open) {
//		this.errors = errors;
		this.builder = builder;
		this.open = open;
		closer = new ParenCloseRewriter(from);
		curr = new TDAExprReducer(errors, closer);
	}
	
	@Override
	public void term(Expr term) {
		if (term instanceof Punctuator) {
			Punctuator punc = (Punctuator) term;
			if (punc.is(")")) {
				closer.endAt(term);
				curr.done();
			} else if (punc.is(",")) {
				System.out.println("is comma");
				if (open.is("("))
					curr.asTuple(open.location());
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

}
