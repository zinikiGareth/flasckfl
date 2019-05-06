package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAFunctionCaseParser implements TDAParsing {
	private final ErrorReporter errors;

	public TDAFunctionCaseParser(ErrorReporter errors, ParsedLineConsumer consumer) {
		this.errors = errors;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void scopeComplete(InputPosition location) {
		errors.message(location, "no function cases specified");
	}
}
