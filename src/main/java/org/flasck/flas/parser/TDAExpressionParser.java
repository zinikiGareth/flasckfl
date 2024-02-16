package org.flasck.flas.parser;

import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.ut.IntroduceNamer;
import org.flasck.flas.parser.ut.IntroductionConsumer;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAExpressionParser implements TDAParsing {
	public static class Builder implements ExprTermConsumer {
		private final Consumer<Expr> handler;

		public Builder(Consumer<Expr> handler) {
			this.handler = handler;
		}

		@Override
		public boolean isTop() {
			return false;
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
		this(errors, null, exprHandler, true, null);
	}

	public TDAExpressionParser(ErrorReporter errors, Consumer<Expr> exprHandler, boolean reduceToOne) {
		ExprReducerErrors ere = new ExprReducerErrors(errors);
		this.parser = new TDAExprParser(errors, ere, null, new TDAStackReducer(ere, new Builder(exprHandler), reduceToOne), null);
	}

	public TDAExpressionParser(ErrorReporter errors, IntroduceNamer namer, Consumer<Expr> exprHandler, boolean reduceToOne, IntroductionConsumer consumer) {
		ExprReducerErrors ere = new ExprReducerErrors(errors);
		this.parser = new TDAExprParser(errors, ere, namer, new TDAStackReducer(ere, new Builder(exprHandler), reduceToOne), consumer);
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
