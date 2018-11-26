package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAExprParser implements TDAParsing {

	private final ExprConsumer builder;

	public TDAExprParser(ErrorReporter errors, ExprConsumer builder) {
		this.builder = builder;
		// TODO Auto-generated constructor stub
	}

	public TDAParsing tryParsing(Tokenizable line) {
		builder.term(new UnresolvedVar(new InputPosition("-", 1, 0, ""), "x"));
		return null;
	}

}
