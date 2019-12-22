package org.flasck.flas.parser;

import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAExpressionParser implements TDAParsing {
	public static class Builder implements ExprTermConsumer {
		private final Consumer<Expr> handler;

		public Builder(Consumer<Expr> handler) {
			this.handler = handler;
		}

		@Override
		public void term(Expr term) {
			handler.accept(term);
		}

		@Override
		public void done() {
			// handler.done()?
		}

		@Override
		public void showStack(StackDumper d) {
			throw new org.zinutils.exceptions.NotImplementedException();
		}
	}

	private final TDAExprParser parser;

	public TDAExpressionParser(ErrorReporter errors, Consumer<Expr> exprHandler) {
		this(errors, exprHandler, true);
	}

	public TDAExpressionParser(ErrorReporter errors, Consumer<Expr> exprHandler, boolean reduceToOne) {
		this.parser = new TDAExprParser(errors, new TDAStackReducer(errors, new Builder(exprHandler), reduceToOne));
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		return parser.tryParsing(toks);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}
