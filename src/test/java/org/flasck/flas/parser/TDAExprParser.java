package org.flasck.flas.parser;

import org.flasck.flas.commonBase.NumericLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAExprParser implements TDAParsing {

	private final ExprConsumer builder;

	public TDAExprParser(ErrorReporter errors, ExprConsumer builder) {
		this.builder = builder;
	}

	public TDAParsing tryParsing(Tokenizable line) {
		ExprToken tok = ExprToken.from(line);
		switch (tok.type) {
		case ExprToken.NUMBER:
			builder.term(new NumericLiteral(tok.location, tok.text, -1));
			return null;
		case ExprToken.IDENTIFIER:
			builder.term(new UnresolvedVar(tok.location, tok.text));
			return null;
		default:
			throw new RuntimeException("Not found");
		}
	}

}
