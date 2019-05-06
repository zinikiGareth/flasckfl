package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.tokenizers.Tokenizable;

public interface TDAParsing {
	TDAParsing tryParsing(Tokenizable toks);
	void scopeComplete(InputPosition location);
}
