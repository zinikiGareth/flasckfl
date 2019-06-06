package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tokenizers.Tokenizable;

public class LastActionScopeParser implements LastOneOnlyNestedParser {
	private final TDAParsing parser;

	public LastActionScopeParser(ErrorReporter errors, FunctionNameProvider namer, FunctionScopeUnitConsumer topLevel) {
		this.parser = TDAMultiParser.functionScopeUnit(errors, namer, topLevel, topLevel);
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		return parser.tryParsing(toks);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}
