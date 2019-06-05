package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.tokenizers.Tokenizable;

public class IgnoreNestedParser implements TDAParsing {

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		return this;
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
