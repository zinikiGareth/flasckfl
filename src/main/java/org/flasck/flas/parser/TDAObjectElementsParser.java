package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAObjectElementsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final ObjectElementsConsumer builder;

	public TDAObjectElementsParser(ErrorReporter errors, ObjectElementsConsumer od) {
		this.errors = errors;
		this.builder = od;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		switch (kw.text) {
		case "state": {
			if (toks.hasMore()) {
				errors.message(toks, "extra characters at end of line");
				return new IgnoreNestedParser();
			}
			StateDefinition state = new StateDefinition(toks.realinfo());
			builder.defineState(state);
			return new TDAStructFieldParser(errors, state);
		}
		case "ctor": {
			ObjectCtor ctor = new ObjectCtor();
			builder.addConstructor(ctor);
			return new TDAMethodMessageParser();
		}
		default: {
			errors.message(toks, "'" + kw.text + "' is not a valid object keyword");
			return new IgnoreNestedParser();
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
