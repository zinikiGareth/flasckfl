package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;
import org.zinutils.exceptions.NotImplementedException;

public class TDACardElementsParser implements TDAParsing, FunctionNameProvider {
	private final ErrorReporter errors;
	private final CardElementsConsumer consumer;
	private final TopLevelDefinitionConsumer topLevel;
	private boolean seenState;

	public TDACardElementsParser(ErrorReporter errors, CardElementsConsumer consumer, TopLevelDefinitionConsumer topLevel) {
		this.errors = errors;
		this.consumer = consumer;
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
		case "template": {
			TemplateNameToken tn = TemplateNameToken.from(toks);
			consumer.addTemplate(new Template(kw.location, tn.location, consumer.templateName(tn.text), null, null));
			// TODO: this ISN'T right, but there isn't a test making me do anything else yet ...
			return new NoNestingParser(errors);
		}
		case "event": {
			ValidIdentifierToken var = VarNameToken.from(toks);
			FunctionName fnName = FunctionName.eventMethod(var.location, consumer.cardName(), var.text);
			MethodConsumer evConsumer = em -> { consumer.addEventHandler(em); };
			return new TDAMethodParser(errors, this, evConsumer, topLevel).parseMethod(fnName, toks);
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
