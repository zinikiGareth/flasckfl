package org.flasck.flas.parser;

import java.util.function.Consumer;

import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ConstructorMatch;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.ConstructorMatch.Field;
import org.flasck.flas.parsedForm.TypedPattern;
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
				PattToken first = PattToken.from(toks);
				if (first == null) {
					errors.message(toks, "invalid pattern");
					return null;
				}
				switch (first.type) {
					case PattToken.VAR: {
						consumer.accept(new VarPattern(first.location, first.text));
						break;
					}
					case PattToken.TYPE: {
						PattToken var = PattToken.from(toks);
						TypeReference type = new TypeReference(first.location, first.text);
						TypedPattern m = new TypedPattern(first.location, type, var.location, var.text);
						consumer.accept(m);
						break;
					}
					default: {
						errors.message(toks, "invalid pattern");
						return null;
					}
				}
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
