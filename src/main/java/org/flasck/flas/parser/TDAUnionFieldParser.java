package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAUnionFieldParser implements TDAParsing {
	private final ErrorReporter errors;
	private final UnionFieldConsumer consumer;

	public TDAUnionFieldParser(ErrorReporter errors, UnionFieldConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		TypeReference type = (TypeReference) new TypeExprParser().tryParsing(toks);
		if (type == null) {
			errors.message(toks, "field must have a valid type definition");
			return new IgnoreNestedParser();
		}
		if (toks.hasMoreContent()) {
			errors.message(toks, "tokens beyond end of line");
			return new IgnoreNestedParser();
		}
		consumer.addCase(type);
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
