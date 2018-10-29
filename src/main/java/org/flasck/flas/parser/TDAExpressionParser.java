package org.flasck.flas.parser;

import java.util.function.Consumer;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAExpressionParser implements TDAParsing {
	private final ErrorReporter errors;
	private final Consumer<Object> exprHandler;
	private final Expression parser;

	public TDAExpressionParser(ErrorReporter errors, Consumer<Object> exprHandler) {
		this.errors = errors;
		this.exprHandler = exprHandler;
		this.parser = new Expression();
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		// TDA
		Object o = parser.tryParsing(toks);
		if (o == null)
			errors.message(toks, "not a valid expression");
		else if (o instanceof ErrorReporter)
			errors.merge((ErrorReporter)o);
		else if (toks.hasMore())
			errors.message(toks, "invalid tokens after expression");
		else
			exprHandler.accept(o);
		return null; // I can't think why anybody would use this - we aren't even parsing a complete line
	}

}
