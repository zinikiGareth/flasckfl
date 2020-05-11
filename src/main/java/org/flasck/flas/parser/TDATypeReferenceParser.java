package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.PolyType;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.PattToken;
import org.flasck.flas.tokenizers.PolyTypeToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class TDATypeReferenceParser implements TDAParsing {
	private final ErrorReporter errors;
	private final VarNamer namer;
	private final Consumer<TypeReference> consumer;
	private final FunctionScopeUnitConsumer topLevel;
	
	public TDATypeReferenceParser(ErrorReporter errors, VarNamer namer, Consumer<TypeReference> consumer, FunctionScopeUnitConsumer topLevel) {
		this.errors = errors;
		this.namer = namer;
		this.consumer = consumer;
		this.topLevel = topLevel;
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
			TDATypeReferenceParser inner = new TDATypeReferenceParser(errors, namer, x -> andTypeParameters.add(x), topLevel);
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
		PolyType pt = PolyTypeToken.fromToken(tok.location, namer, qn.text);
		if (pt != null)
			topLevel.polytype(errors, pt);
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

}
