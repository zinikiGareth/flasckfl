package org.flasck.flas.parser.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.assembly.RoutingActions;
import org.flasck.flas.parsedForm.assembly.SubRouting;
import org.flasck.flas.parser.BlockLocationTracker;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TDARoutingParser extends BlockLocationTracker implements TDAParsing {
	private final RoutingGroupConsumer consumer;

	public TDARoutingParser(ErrorReporter errors, RoutingGroupConsumer consumer, LocationTracker parentTracker) {
		super(errors, parentTracker);
		this.consumer = consumer;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		ValidIdentifierToken kw = VarNameToken.from(errors, toks);
		if (kw == null) {
			errors.message(toks, "expected routing keyword or card var");
			return new IgnoreNestedParser(errors);
		}
		
		toks.skipWS(errors);
		switch (kw.text) {
		case "enter": {
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser(errors);
			}
			RoutingActions enter = new RoutingActions(kw.location);
			consumer.enter(enter);
			errors.logReduction("fa-route-enter", kw.location, kw.location);
			super.tellParent(kw.location);
			return new TDAParsingWithAction(new TDAEnterExitParser(errors, enter, this), reduction(kw.location, "assembly-route-enter"));
		}
		case "at": {
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser(errors);
			}
			RoutingActions at = new RoutingActions(kw.location);
			consumer.at(at);
			errors.logReduction("fa-route-at", kw.location, kw.location);
			super.tellParent(kw.location);
			return new TDAParsingWithAction(new TDAEnterExitParser(errors, at, this), reduction(kw.location, "assembly-route-at"));
		}
		case "exit": {
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser(errors);
			}
			RoutingActions exit = new RoutingActions(kw.location);
			consumer.exit(exit);
			errors.logReduction("fa-route-exit", kw.location, kw.location);
			super.tellParent(kw.location);
			return new TDAParsingWithAction(new TDAEnterExitParser(errors, exit, this), reduction(kw.location, "assembly-route-exit"));
		}
		case "secure": {
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser(errors);
			}
			consumer.isSecure();
			errors.logReduction("assembly-route-secure", kw.location, kw.location);
			super.tellParent(kw.location);
			return new NoNestingParser(errors);
		}
		case "route": {
			int mark = toks.at();
			InputPosition pos = toks.realinfo();
			String s = StringToken.from(errors, toks);
			if (s == null) {
				errors.message(toks, "must specify a route path");
				return new IgnoreNestedParser(errors);
			}
			errors.logParsingToken(new ExprToken(pos, ExprToken.STRING, s).original(toks.fromMark(mark)));
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser(errors);
			}
			RoutingGroupConsumer group = new SubRouting(errors, pos, s, consumer, new SubRouteName(consumer.name(), s)); // TODO: do we possibly need to modify the route path to become a name?
			consumer.route(group);
			errors.logReduction("fa-route-nested", kw.location, pos);
			super.tellParent(kw.location);
			return new TDAParsingWithAction(new TDARoutingParser(errors, group, this), reduction(kw.location, "assembly-route-nested"));
		}
		case "title": {
			int mark = toks.at();
			InputPosition pos = toks.realinfo();
			String s = StringToken.from(errors, toks);
			if (s == null) {
				errors.message(toks, "must specify a title");
				return new IgnoreNestedParser(errors);
			}
			errors.logParsingToken(new ExprToken(pos, ExprToken.STRING, s).original(toks.fromMark(mark)));
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser(errors);
			}
			consumer.title(pos, s);
			errors.logReduction("assembly-title", kw.location, pos);
			super.tellParent(kw.location);
			return new NoNestingParser(errors);
		}
		default: {
			ExprToken op = ExprToken.from(errors, toks);
			if (op == null || !"<-".equals(op.text)) {
				errors.message(toks, "expected 'enter', 'at', 'exit', 'secure', 'route', 'query', 'title' or card assignment");
				return new IgnoreNestedParser(errors);
			}
			if (kw.text.equals("main") && !(consumer instanceof MainRoutingGroupConsumer)) {
				errors.message(kw.location, "main cannot be set here");
				return new IgnoreNestedParser(errors);
			}
			TypeNameToken card = TypeNameToken.qualified(errors, toks);
			if (card == null) {
				errors.message(toks, "card name required");
				return new IgnoreNestedParser(errors);
			}
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "junk at end of line");
				return new IgnoreNestedParser(errors);
			}
			TypeReference tr = new TypeReference(card.location, card.text);
			consumer.assignCard(new UnresolvedVar(kw.location, kw.text), tr);
			if (kw.text.equals("main"))
				((MainRoutingGroupConsumer) consumer).provideMainCard(tr);
			errors.logReduction("fa-route-action", kw.location, card.location);
			super.tellParent(kw.location);
			return new NoNestingParser(errors);
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
