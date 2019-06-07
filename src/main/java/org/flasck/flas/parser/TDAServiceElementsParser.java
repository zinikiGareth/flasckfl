package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ServiceDefinition;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;

public class TDAServiceElementsParser implements TDAParsing, FunctionNameProvider {
	private final ErrorReporter errors;
	private final ServiceDefinition consumer;
	private final TopLevelDefinitionConsumer topLevel;
	private boolean seenState;

	public TDAServiceElementsParser(ErrorReporter errors, ServiceDefinition service, TopLevelDefinitionConsumer topLevel) {
		this.errors = errors;
		this.consumer = service;
		this.topLevel = topLevel;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		switch (kw.text) {
		case "state": {
			if (seenState) {
				errors.message(kw.location, "multiple state declarations");
				return null;
			}
			final StateDefinition state = new StateDefinition(toks.realinfo());
			consumer.defineState(state);
			seenState = true;
			
			return new TDAStructFieldParser(errors, state);
		}
		default:
			throw new NotImplementedException();
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public FunctionName functionName(InputPosition location, String base) {
		return FunctionName.function(location, consumer.cardName(), base);
	}

}
