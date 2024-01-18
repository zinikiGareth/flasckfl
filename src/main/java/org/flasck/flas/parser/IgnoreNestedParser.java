package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.tokenizers.CommentToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class IgnoreNestedParser implements TDAParsing {
	private final ErrorReporter errors;

	public IgnoreNestedParser(ErrorReporter errors) {
		this.errors = errors;
	}
	
	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		InputPosition pos = toks.realinfo();
		errors.logParsingToken(new CommentToken(pos, toks.remainder()));
		return this;
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
