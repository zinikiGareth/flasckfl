package org.flasck.flas.parser;

import java.util.function.Consumer;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.PattToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAPatternParser implements TDAParsing {
	private final Consumer<Object> consumer;

	public TDAPatternParser(ErrorReporter errors, Consumer<Object> consumer) {
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		PattToken tok = PattToken.from(toks);
		if (tok == null)
			return null;

		if (tok.type == PattToken.VAR) {
			consumer.accept(new VarPattern(tok.location, tok.text));
		}
		
		return this;
	}
}
