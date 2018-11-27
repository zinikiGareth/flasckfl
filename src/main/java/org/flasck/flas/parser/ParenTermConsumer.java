package org.flasck.flas.parser;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;

public class ParenTermConsumer implements ExprTermConsumer {
	private final ErrorReporter errors;
	private final ExprTermConsumer builder;
	private TDAExprReducer curr;

	public ParenTermConsumer(ErrorReporter errors, ExprTermConsumer builder) {
		this.errors = errors;
		this.builder = builder;
		curr = new TDAExprReducer(null, builder);
	}
	
	@Override
	public void term(Expr term) {
		if (term instanceof Punctuator && ((Punctuator)term).is(")"))
			curr.done();
		else
			curr.term(term);
	}

	@Override
	public void done() {
		throw new RuntimeException("I don't think we should get here");
	}

}
