package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.tokenizers.Tokenizable;

public class IgnoreNestedParser implements TDAParsing {

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
