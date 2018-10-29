package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.tokenizers.Tokenizable;

public class NoNestingParser implements TDAParsing {
	private final ErrorReporter errors;

	public NoNestingParser(ErrorReporter errors) {
		this.errors = errors;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		errors.message(toks, "a nested block is not allowed here");
		return null;
	}

}
