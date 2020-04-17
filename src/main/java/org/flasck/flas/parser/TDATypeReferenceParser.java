package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.PattToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class TDATypeReferenceParser implements TDAParsing {
	private final ErrorReporter errors;
	private final Consumer<TypeReference> consumer;
	
	public TDATypeReferenceParser(ErrorReporter errors, Consumer<TypeReference> consumer) {
		super();
		this.errors = errors;
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		TypeNameToken qn = TypeNameToken.qualified(toks);
		if (qn == null) {
			errors.message(toks, "typename expected");
			return null;
		}
		int mark = toks.at();
		PattToken tok = PattToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "object name expected");
			return null;
		}
		List<TypeReference> andTypeParameters = new ArrayList<>();
		if (tok.type == PattToken.OSB) {
			TDATypeReferenceParser inner = new TDATypeReferenceParser(errors, x -> andTypeParameters.add(x));
			while (true) {
				if (inner.tryParsing(toks) == null) {
					// it failed, we fail ...
					return null;
				}
				tok = PattToken.from(errors, toks);
				if (tok.type == PattToken.COMMA)
					continue;
				else if (tok.type == PattToken.CSB)
					break;
				else {
					errors.message(toks, "invalid pattern");
					return null;
				}
			}
		} else {
			// whatever it was, we didn't want it, so put it back in the pool for somebody else
			toks.reset(mark);
		}
		consumer.accept(new TypeReference(qn.location, qn.text, andTypeParameters));
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}
