package org.flasck.flas.parser.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.assembly.RoutingActions;
import org.flasck.flas.parsedForm.assembly.SubRouting;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;

public class TDARoutingParser implements TDAParsing {
	private final ErrorReporter errors;
	private final RoutingGroupConsumer consumer;

	public TDARoutingParser(ErrorReporter errors, RoutingGroupConsumer consumer) {
		this.errors = errors;
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(toks);
		if (kw == null) {
			errors.message(toks, "expected routing keyword");
			return new IgnoreNestedParser();
		}
		
		switch (kw.text) {
		case "enter": {
			if (toks.hasMoreContent()) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser();
			}
			RoutingActions enter = new RoutingActions(kw.location);
			consumer.enter(enter);
			return new TDAEnterExitParser(errors, enter);
		}
		case "exit": {
			if (toks.hasMoreContent()) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser();
			}
			RoutingActions exit = new RoutingActions(kw.location);
			consumer.exit(exit);
			return new TDAEnterExitParser(errors, exit);
		}
		case "route": {
			String s = StringToken.from(errors, toks);
			if (s == null) {
				errors.message(toks, "must specify a route path");
				return new IgnoreNestedParser();
			}
			if (toks.hasMoreContent()) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser();
			}
			RoutingGroupConsumer group = new SubRouting(errors, s, consumer);
			consumer.route(group);
			return new TDARoutingParser(errors, group);
		}
		default: {
			ExprToken op = ExprToken.from(errors, toks);
			if (op == null || !"<-".equals(op.text)) {
				errors.message(toks, "expected 'enter', 'at', 'exit', 'route' or card assignment");
				return new IgnoreNestedParser();
			}
			if (kw.text.equals("main") && !(consumer instanceof MainRoutingGroupConsumer)) {
				errors.message(kw.location, "main cannot be set here");
				return new IgnoreNestedParser();
			} else if (!kw.text.equals("main") && (consumer instanceof MainRoutingGroupConsumer)) {
				errors.message(kw.location, "top level card must be called main");
				return new IgnoreNestedParser();
			}
			TypeNameToken card = TypeNameToken.qualified(toks);
			if (card == null) {
				errors.message(toks, "card name required");
				return new IgnoreNestedParser();
			}
			if (toks.hasMoreContent()) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser();
			}
			TypeReference tr = new TypeReference(card.location, card.text);
			consumer.assignCard(new UnresolvedVar(kw.location, kw.text), tr);
			if (consumer instanceof MainRoutingGroupConsumer)
				((MainRoutingGroupConsumer) consumer).provideMainCard(tr);
			return new NoNestingParser(errors);
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
		// TODO Auto-generated method stub

	}

}
