package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

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
		TypeExprParser parser = new TypeExprParser(errors);
		List<TypeReference> types = new ArrayList<>();
		TDAProvideType pt = ty -> types.add(ty);
		parser.tryParsing(toks, pt);
		if (types.isEmpty()) {
			errors.message(toks, "field must have a valid type definition");
			return new IgnoreNestedParser();
		}
		if (toks.hasMoreContent()) {
			errors.message(toks, "tokens beyond end of line");
			return new IgnoreNestedParser();
		}
		consumer.addCase(types.get(0));
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
