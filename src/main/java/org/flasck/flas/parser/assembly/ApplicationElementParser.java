package org.flasck.flas.parser.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class ApplicationElementParser implements TDAParsing {
	private final ErrorReporter errors;
	private final ApplicationElementConsumer consumer;

	public ApplicationElementParser(ErrorReporter errors, ApplicationElementConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		if (kw == null) {
			errors.message(toks, "expected assembly keyword");
			return new IgnoreNestedParser();
		}
		
		switch (kw.text) {
		case "title": {
			String s = StringToken.from(errors, toks);
			consumer.title(s);
			return new NoNestingParser(errors);
		}
		default: {
			errors.message(toks, "expected 'application' or 'card'");
			return new IgnoreNestedParser();
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		// TODO Auto-generated method stub

	}

}
