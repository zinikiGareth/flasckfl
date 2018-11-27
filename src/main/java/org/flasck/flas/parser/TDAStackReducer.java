package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;

// TODO: introduce a "just one" error reporter that we can use to make sure we don't overly complain about missing parens, etc.
public class TDAStackReducer implements ExprTermConsumer {
	public class ParentConsumer implements ExprTermConsumer {

		@Override
		public void term(Expr term) {
			stack.remove(0);
			if (stack.isEmpty())
				throw new RuntimeException("Stack underflow - should be error");
			stack.get(0).term(term);
		}

		@Override
		public void done() {
			throw new org.zinutils.exceptions.NotImplementedException();
		}

	}

	private final ErrorReporter errors;
	private final List<ExprTermConsumer> stack = new ArrayList<>();

	public TDAStackReducer(ErrorReporter errors, ExprTermConsumer builder) {
		this.errors = errors;
		this.stack.add(new TDAExprReducer(errors, builder));
	}

	@Override
	public void term(Expr term) {
		if (term instanceof Punctuator) {
			ExprTermConsumer n = ((Punctuator)term).isOpen(errors, new ParentConsumer());
			if (n != null) {
				this.stack.add(0, n);
				return;
			}
		}
		this.stack.get(0).term(term);
	}

	@Override
	public void done() {
		if (this.stack.size() != 1)
			throw new RuntimeException("Stack too deep - should be error");
		this.stack.get(0).done();
	}
}
