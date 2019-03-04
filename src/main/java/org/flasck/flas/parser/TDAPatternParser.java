package org.flasck.flas.parser;

import java.util.function.Consumer;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.PattToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAPatternParser implements TDAParsing {
	private final ErrorReporter errors;
	private final Consumer<Pattern> consumer;

	public TDAPatternParser(ErrorReporter errors, Consumer<Pattern> consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		PattToken tok = PattToken.from(toks);
		if (tok == null)
			return null;

		switch (tok.type) {
			case PattToken.VAR: {
				consumer.accept(new VarPattern(tok.location, tok.text));
				break;
			}
			default: {
				errors.message(toks, "invalid pattern");
				return null;
			}
		}
		
		return this;
	}
}
