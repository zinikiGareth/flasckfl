package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.Template;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.zinutils.exceptions.NotImplementedException;

public class TDACardElementsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final CardElementsConsumer consumer;
	private boolean seenState;

	public TDACardElementsParser(ErrorReporter errors, CardElementsConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
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
		default:
			throw new NotImplementedException();
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}
