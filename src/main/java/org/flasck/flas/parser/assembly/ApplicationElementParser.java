package org.flasck.flas.parser.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class ApplicationElementParser implements TDAParsing {
	private final ErrorReporter errors;
	private final InputPosition startPos;
	private final ApplicationElementConsumer consumer;
	private boolean sawMainCard;

	public ApplicationElementParser(ErrorReporter errors, InputPosition startPos, ApplicationElementConsumer consumer) {
		this.errors = errors;
		this.startPos = startPos;
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
		case "main": {
			sawMainCard = true;
			TypeNameToken main = TypeNameToken.unqualified(toks);
			if (main == null) {
				errors.message(toks, "no main card was specified");
				return new IgnoreNestedParser();
			}
			consumer.mainCard(main.text);
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
		if (!sawMainCard)
			errors.message(startPos, "assembly must identify a main card");
	}

}
