package org.flasck.flas.parser.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class TDARoutingParser implements TDAParsing {
	private final ErrorReporter errors;
	private final RoutingActionConsumer consumer;

	public TDARoutingParser(ErrorReporter errors, RoutingActionConsumer consumer) {
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
		case "enter": {
			if (toks.hasMoreContent()) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser();
			}
			return new TDAEnterExitParser(errors, consumer);
		}
		case "exit": {
			if (toks.hasMoreContent()) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser();
			}
			return new TDAEnterExitParser(errors, consumer);
		}
		case "main": {
			if (!(consumer instanceof MainRoutingActionConsumer)) {
				errors.message(kw.location, "main cannot be set here");
				return new IgnoreNestedParser();
			}
			ExprToken op = ExprToken.from(errors, toks);
			if (op == null) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			if (!"<-".equals(op.text)) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			TypeNameToken card = TypeNameToken.qualified(toks);
			if (card == null) {
				errors.message(toks, "card name required");
				return new IgnoreNestedParser();
			}
			TypeReference tr = new TypeReference(card.location, card.text);
			((MainRoutingActionConsumer) consumer).provideMainCard(tr);
			consumer.assignCard(new UnresolvedVar(kw.location, kw.text), tr);
			if (toks.hasMoreContent()) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser();
			}
			return new NoNestingParser(errors);
		}
		default: {
			errors.message(toks, "expected 'main', 'enter', 'at', 'exit' or 'route'");
			return new IgnoreNestedParser();
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		// TODO Auto-generated method stub

	}

}
