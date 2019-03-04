package org.flasck.flas.parser;

import java.util.function.Consumer;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ConstructorMatch;
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
			case PattToken.TYPE: {
				consumer.accept(new ConstructorMatch(tok.location, tok.text));
				break;
			}
			case PattToken.ORB: {
				PattToken var = PattToken.from(toks);
				if (var == null || var.type != PattToken.VAR) {
					errors.message(toks, "invalid pattern");
					return null;
				}
				consumer.accept(new VarPattern(var.location, var.text));
				PattToken crb = PattToken.from(toks);
				if (crb == null || crb.type != PattToken.CRB) {
					errors.message(toks, "invalid pattern");
					return null;
				}
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
