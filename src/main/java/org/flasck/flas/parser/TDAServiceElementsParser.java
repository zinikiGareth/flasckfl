package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractService;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.FieldsDefn.FieldsType;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class TDAServiceElementsParser implements TDAParsing, FunctionNameProvider, HandlerNameProvider {
	private final ErrorReporter errors;
	private final ServiceElementsConsumer consumer;
	private final TopLevelDefinitionConsumer topLevel;
	private boolean seenState;

	public TDAServiceElementsParser(ErrorReporter errors, ServiceElementsConsumer service, TopLevelDefinitionConsumer topLevel) {
		this.errors = errors;
		this.consumer = service;
		this.topLevel = topLevel;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		if (kw == null)
			return null;
		switch (kw.text) {
		case "state": {
			if (seenState) {
				errors.message(kw.location, "multiple state declarations");
				return new IgnoreNestedParser();
			}
			final StateDefinition state = new StateDefinition(toks.realinfo());
			consumer.defineState(state);
			seenState = true;
			
			return new TDAStructFieldParser(errors, state, FieldsType.STATE);
		}
		case "method": {
			FunctionNameProvider namer = (loc, text) -> FunctionName.standaloneMethod(loc, consumer.cardName(), text);
			MethodConsumer smConsumer = sm -> { topLevel.newStandaloneMethod(sm); };
			return new TDAMethodParser(errors, this, this, smConsumer, topLevel).parseMethod(namer, toks);
		}
		case "provides": {
			TypeNameToken tn = TypeNameToken.qualified(toks);
			if (tn == null) {
				errors.message(toks, "invalid contract reference");
				return new IgnoreNestedParser();
			}
			if (toks.hasMore()) {
				errors.message(toks, "extra tokens at end of line");
				return new IgnoreNestedParser();
			}
			final ContractService cs = new ContractService(kw.location, tn.location, tn.text, null, null);
			consumer.addProvidedService(cs);
			return new TDAImplementationMethodsParser(errors, (loc, text) -> FunctionName.contractMethod(loc, cs.getRealName(), text), cs, topLevel);
		}
		default:
			return null;
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

	@Override
	public HandlerName provide(String baseName) {
		return new HandlerName(consumer.cardName(), baseName);
	}

}
