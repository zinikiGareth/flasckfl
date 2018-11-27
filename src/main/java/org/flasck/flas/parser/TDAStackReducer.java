package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;

public class TDAStackReducer implements ExprTermConsumer {
	private final ErrorReporter errors;
	private final ExprTermConsumer builder;
	private final List<ExprTermConsumer> stack = new ArrayList<>();

	public TDAStackReducer(ErrorReporter errors, ExprTermConsumer builder) {
		this.errors = errors;
		this.builder = builder;
		this.stack.add(new TDAExprReducer(errors, builder));
	}

	@Override
	public void term(Expr term) {
		this.stack.get(0).term(term);
	}

	@Override
	public void done() {
		this.stack.get(0).done();
	}
}
