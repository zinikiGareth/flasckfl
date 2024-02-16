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
	private final VarNamer namer;
	private final boolean reduceSimple;
	private final Consumer<TypeReference> consumer;
	private final FunctionScopeUnitConsumer topLevel;
	
	public TDATypeReferenceParser(ErrorReporter errors, VarNamer namer, boolean reduceSimple, Consumer<TypeReference> consumer, FunctionScopeUnitConsumer topLevel) {
		this.errors = errors;
		this.namer = namer;
		this.reduceSimple = reduceSimple;
		this.consumer = consumer;
		this.topLevel = topLevel;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		TypeNameToken qn = TypeNameToken.qualified(errors, toks);
		if (qn == null) {
			errors.message(toks, "typename expected");
			return null;
		}
		int mark = toks.at();
		List<TypeReference> andTypeParameters = new ArrayList<>();
		if (toks.hasMore() && !Character.isWhitespace(toks.nextChar())) {
			PattToken tok = PattToken.from(errors, toks);
			if (tok != null) {
				if (tok.type == PattToken.OSB) {
					TDATypeReferenceParser inner = new TDATypeReferenceParser(errors, namer, false, x -> andTypeParameters.add(x), topLevel);
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
					errors.logReduction("simple-type-name-with-polys", qn.location, andTypeParameters.get(andTypeParameters.size()-1).location());
				} else {
					// whatever it was, we didn't want it, so put it back in the pool for somebody else
					toks.reset(mark);
					
					if (reduceSimple)
						errors.logReduction("simple-type-name", qn.location, qn.location.locAtEnd());
				}
			}
		} else if (reduceSimple) {
			errors.logReduction("simple-type-name", qn.location, qn.location.locAtEnd());
		}
		consumer.accept(new TypeReference(qn.location, qn.text, andTypeParameters));
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}
