package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDATemplateOptionsParser implements TDAParsing {
	private final ErrorReporter errors;

	public TDATemplateOptionsParser(ErrorReporter errors) {
		this.errors = errors;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}
