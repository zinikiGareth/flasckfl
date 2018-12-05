package org.flasck.flas.parser;

import java.util.function.Consumer;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.tokenizers.PattToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAPatternParser {
	private final Tokenizable toks;
	private final Consumer<Object> consumer;

	public TDAPatternParser(ErrorReporter errors, Tokenizable toks, Consumer<Object> consumer) {
		this.toks = toks;
		this.consumer = consumer;
	}

	public boolean parse() {
		PattToken tok = PattToken.from(toks);
		if (tok == null)
			return false;

		if (tok.type == PattToken.VAR) {
			consumer.accept(new VarPattern(tok.location, tok.text));
		}
		
		return true;
	}

}
