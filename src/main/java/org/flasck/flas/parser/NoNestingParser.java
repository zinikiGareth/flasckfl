package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.tokenizers.Tokenizable;

public class NoNestingParser implements TDAParsing {
	private final ErrorReporter errors;
	private final String msg;

	public NoNestingParser(ErrorReporter errors) {
		this(errors, "a nested block is not allowed here");
	}
	
	public NoNestingParser(ErrorReporter errors, String msg) {
		this.errors = errors;
		this.msg = msg;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		errors.message(toks, msg);
		return null;
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
