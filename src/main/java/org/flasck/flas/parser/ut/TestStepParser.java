package org.flasck.flas.parser.ut;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.MatchedItem;
import org.flasck.flas.parser.BlockLocationTracker;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.ParenExprConsumer;
import org.flasck.flas.parser.TDAExpressionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tokenizers.EventZoneToken;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.PuncToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;
import org.zinutils.collections.TriConsumer;

public class TestStepParser extends BlockLocationTracker implements TDAParsing {
	protected final UnitTestStepConsumer builder;
	protected final UnitDataNamer namer;
	private final UnitTestDefinitionConsumer topLevel;

	public TestStepParser(ErrorReporter errors, UnitDataNamer namer, UnitTestStepConsumer builder, UnitTestDefinitionConsumer topLevel, LocationTracker locTracker) {
		super(errors, locTracker);
		this.namer = namer;
		this.builder = builder;
		this.topLevel = topLevel;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		int mark = toks.at();
		KeywordToken kw = KeywordToken.from(errors, toks);
		if (kw == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		updateLoc(kw.location);
		switch (kw.text) {
		case "assert": {
			return handleAssert(kw, toks);
		}
		case "identical": {
			return handleIdentical(kw, toks);
		}
		case "shove": {
			return handleShove(kw, toks);
		}
		case "close": {
			return closeCard(kw, toks);
		}
		case "contract": {
			return handleSendToContract(kw, toks);
		}
		case "data": {
			return handleDataDecl(kw, toks);
		}
		case "newdiv": {
			return handleNewdiv(kw, toks);
		}
		case "render": {
			return handleRender(kw, toks);
		}
		case "event": {
			return handleEvent(kw, toks);
		}
		case "input": {
			return handleInput(kw, toks);
		}
		case "invoke": {
			return handleInvoke(kw, toks);
		}
		case "expect": {
			return handleExpect(kw, toks);
		}
		case "match": {
			return handleMatch(kw, toks);
		}
		default: {
			toks.reset(mark);
			errors.message(toks, "unrecognized test step " + kw.text);
			return new IgnoreNestedParser(errors);
		}
		}
	}

	protected TDAParsing handleAssert(KeywordToken kw, Tokenizable toks) {
		List<Expr> test = new ArrayList<>();
		ParenExprConsumer pec = new ParenExprConsumer(x -> test.add(x));
		TDAExpressionParser expr = new TDAExpressionParser(errors, pec);
		expr.tryParsing(toks);
		if (errors.hasErrors()){
			return new IgnoreNestedParser(errors);
		}
		if (test.isEmpty()) {
			errors.message(toks, "assert requires expression to evaluate");
			return new IgnoreNestedParser(errors);
		}
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		errors.logReduction("ut-assert-expr", kw.location, pec.location());
		Consumer<Expr> exprConsumer = ex -> {
			builder.assertion(test.get(0), ex);
			updateLoc(ex.location());
			errors.logReduction("ut-assert-expected-value", ex, ex);
			reduce(kw.location, "unit-test-assert");
		};
		return new SingleExpressionParser(errors, "assert", exprConsumer, this);
	}

	protected TDAParsing handleIdentical(KeywordToken kw, Tokenizable toks) {
		List<Expr> test = new ArrayList<>();
		TDAExpressionParser expr = new TDAExpressionParser(errors, x -> test.add(x));
		expr.tryParsing(toks);
		if (errors.hasErrors()){
			return new IgnoreNestedParser(errors);
		}
		if (test.isEmpty()) {
			errors.message(toks, "identical requires expression to evaluate");
			return new IgnoreNestedParser(errors);
		}
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		errors.logReduction("ut-identical-expr", kw.location, test.get(0).location());
		Consumer<Expr> exprConsumer = ex -> {
			builder.identical(test.get(0), ex);
			updateLoc(ex.location());
			errors.logReduction("ut-identical-expected-value", ex, ex);
			reduce(kw.location, "unit-test-identical");
		};
		return new SingleExpressionParser(errors, "identical", exprConsumer, this);
	}

	protected TDAParsing handleShove(KeywordToken kw, Tokenizable toks) {
		List<UnresolvedVar> slots = new ArrayList<>();
		Locatable haveDot = null, firstMPV = null;
		while (true) {
			ExprToken tok = ExprToken.from(errors, toks);
			if (tok == null || tok.type != ExprToken.IDENTIFIER) {
				errors.message(toks, "field path expected");
				return new IgnoreNestedParser(errors);
			}
			UnresolvedVar v = new UnresolvedVar(tok.location, tok.text);
			slots.add(v);
			if (haveDot != null) {
				if (firstMPV == null)
					firstMPV = v;
				else
					errors.logReduction("member-path-apply", haveDot, v);
			}
			PuncToken dot = PuncToken.from(errors, toks);
			if (dot == null) {
				if (haveDot == null) {
					errors.message(toks, ". expected");
					return new IgnoreNestedParser(errors);
				}
				break;
			}
			if (".".equals(dot.text)) {
				haveDot = dot;
			} else {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser(errors);
			}
		}

		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}

		UnresolvedVar last = slots.get(slots.size()-1);
		errors.logReduction("member-path", firstMPV, last);
		errors.logReduction("test-step-shove", kw, last);

		Consumer<Expr> exprConsumer = expr -> {
			builder.shove(slots, expr); 
			updateLoc(expr.location());
			errors.logReduction("ut-shove-expected-expr", expr.location(), expr.location());
			reduce(kw.location, "unit-test-shove");
		};
		return new SingleExpressionParser(errors, "shove", exprConsumer, this);
	}

	protected TDAParsing handleSendToContract(KeywordToken kw, Tokenizable toks) {
		ValidIdentifierToken tok = VarNameToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "contract requires a card variable to send the event to");
			return new IgnoreNestedParser(errors);
		}
		TypeNameToken evname = TypeNameToken.qualified(errors, toks);
		if (evname == null) {
			errors.message(toks, "contract requires a Contract name");
			return new IgnoreNestedParser(errors);
		}
		Tokenizable ec = toks.copyTo("->");
		List<Expr> eventObj = new ArrayList<>();
		TDAExpressionParser expr = new TDAExpressionParser(errors, x -> eventObj.add(x));
		expr.tryParsing(ec);
		if (errors.hasErrors()){
			return new IgnoreNestedParser(errors);
		}
		if (eventObj.isEmpty()) {
			errors.message(toks, "missing method call");
			return new IgnoreNestedParser(errors);
		}
		if (ec == toks && toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		if (ec != toks) {
			toks.reset(ec.at());
			ExprToken arrow = ExprToken.from(errors, toks);
			if (arrow == null) {
				errors.message(toks, "expected ->");
				return new NoNestingParser(errors);
			}
			ExprToken name = ExprToken.from(errors, toks);
			if (name == null) {
				errors.message(toks, "expected var to store in");
				return new NoNestingParser(errors);
			}
			if (name.type != ExprToken.IDENTIFIER) {
				errors.message(name.location, "expected var");
				return new NoNestingParser(errors);
			}
			UnresolvedVar handler = new UnresolvedVar(name.location, name.text);
			errors.logReduction("unit-contract-handle-expr", arrow, handler);
			errors.logReduction("unit-contract-action-with-handle", kw, arrow);
			builder.sendOnContract(new UnresolvedVar(tok.location, tok.text), new TypeReference(evname.location, evname.text), eventObj.get(0), handler);
		} else {
			errors.logReduction("unit-contract-action", kw, eventObj.get(0));
			builder.sendOnContract(new UnresolvedVar(tok.location, tok.text), new TypeReference(evname.location, evname.text), eventObj.get(0), null);
		}
		tellParent(kw.location);
		return new NoNestingParser(errors);
	}

	protected TDAParsing closeCard(KeywordToken kw, Tokenizable toks) {
		ValidIdentifierToken tok = VarNameToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "close requires a card variable to close");
			return new IgnoreNestedParser(errors);
		}
		builder.closeCard(new UnresolvedVar(tok.location, tok.text));
		errors.logReduction("unit-test-close-card", kw.location, tok.location);
		tellParent(kw.location);
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleDataDecl(KeywordToken kw, Tokenizable toks) {
		Consumer<UnitDataDeclaration> consumer = dd -> {
			builder.data(errors, dd);
			topLevel.nestedData(dd);
			tellParent(kw.location());
		};
		return new TDAUnitTestDataParser(errors, false, kw, namer, consumer, topLevel, this).tryParsing(toks);
	}

	protected TDAParsing handleNewdiv(KeywordToken kw, Tokenizable toks) {
		Integer cnt = null;
		InputPosition cntLoc = null;
		if (toks.hasMoreContent(errors)) {
			ExprToken tok = ExprToken.from(errors, toks);
			if (tok.type != ExprToken.NUMBER) {
				errors.message(toks, "integer required");
				return new IgnoreNestedParser(errors);
			}
			cntLoc = tok.location;
			try {
				cnt = Integer.parseInt(tok.text);
			} catch (NumberFormatException ex) {
				errors.message(toks, "integer required");
				return new IgnoreNestedParser(errors);
			}
		}
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		builder.newdiv(cnt);
		if (cntLoc != null)
			errors.logReduction("test-newdiv-cnt", kw.location, cntLoc);
		else
			errors.logReduction("test-newdiv", kw.location, kw.location);
		tellParent(kw.location);
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleRender(KeywordToken kw, Tokenizable toks) {
		ValidIdentifierToken tok = VarNameToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "must specify a card to be rendered");
			return new IgnoreNestedParser(errors);
		}
		ExprToken arrow = ExprToken.from(errors, toks);
		if (arrow == null || !"=>".equals(arrow.text)) {
			errors.message(toks, "=> expected");
			return new IgnoreNestedParser(errors);
		}
		TemplateNameToken template = TemplateNameToken.from(errors, toks);
		if (template == null)
			return new IgnoreNestedParser(errors);
		errors.logReduction("test-render", kw.location, template.location);
		builder.render(new UnresolvedVar(tok.location, tok.text), new TemplateReference(template.location, namer.template(template.location, template.text)));
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleEvent(KeywordToken kw, Tokenizable toks) {
		ValidIdentifierToken tok = VarNameToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "must specify a card to receive event");
			return new IgnoreNestedParser(errors);
		}
		TargetZone targetZone = parseTargetZone(toks);
		if (targetZone == null) {
			return new IgnoreNestedParser(errors);
		}
		ErrorMark em = errors.mark();
		List<Expr> eventObj = new ArrayList<>();
		TDAExpressionParser expr = new TDAExpressionParser(errors, x -> eventObj.add(x));
		expr.tryParsing(toks);
		if (em.hasMoreNow()){
			return new IgnoreNestedParser(errors);
		}
		if (eventObj.isEmpty()) {
			errors.message(toks, "must provide an event object");
			return new IgnoreNestedParser(errors);
		}
		if (eventObj.size() > 1) {
			errors.message(toks, "only one event object is allowed");
			return new IgnoreNestedParser(errors);
		}
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		builder.event(new UnresolvedVar(tok.location, tok.text), targetZone, eventObj.get(0));
		errors.logReduction("unit-event-action", kw.location, eventObj.get(0).location());
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleInput(KeywordToken kw, Tokenizable toks) {
		ValidIdentifierToken tok = VarNameToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "must specify a card to receive event");
			return new IgnoreNestedParser(errors);
		}
		TargetZone targetZone = parseTargetZone(toks);
		if (targetZone == null) {
			return new IgnoreNestedParser(errors);
		}
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		errors.logReduction("ut-input-line", kw.location, targetZone.location);
		updateLoc(kw.location);
		Consumer<Expr> exprConsumer = text -> {
			builder.input(new UnresolvedVar(tok.location, tok.text), targetZone, text);
			updateLoc(text.location());
			errors.logReduction("ut-input-entry-value", text, text);
			reduce(kw.location, "unit-test-input");
		};
		return new SingleExpressionParser(errors, "input", exprConsumer, this);
	}

	protected TDAParsing handleInvoke(KeywordToken kw, Tokenizable toks) {
		List<Expr> eventObj = new ArrayList<>();
		TDAExpressionParser expr = new TDAExpressionParser(errors, x -> eventObj.add(x));
		expr.tryParsing(toks);
		if (errors.hasErrors()){
			return new IgnoreNestedParser(errors);
		}
		if (eventObj.isEmpty()) {
			errors.message(toks, "missing expression");
			return new IgnoreNestedParser(errors);
		}
		InputPosition lastLoc = eventObj.get(eventObj.size()-1).location();
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		errors.logReduction("unit-invoke-action", kw.location, lastLoc);
		tellParent(kw.location);
		builder.invokeObjectMethod(eventObj.get(0));
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleExpect(KeywordToken kw, Tokenizable toks) {
		ValidIdentifierToken svc = VarNameToken.from(errors, toks);
		if (svc == null) {
			// If we don't have a service name, then it could be a contract.
			// Look for the special operator "<-"
			ExprToken tok = ExprToken.from(errors, toks);
			if (tok != null && tok.type == ExprToken.SYMBOL) {
				if (tok.text.equals("<~"))
					return handleExpectCancel(kw, toks);
				errors.message(toks, "invalid expect operator " + tok.text);
				return new IgnoreNestedParser(errors);
			}
			errors.message(toks, "missing contract");
			return new IgnoreNestedParser(errors);
		}
		String ih = "";
		ValidIdentifierToken meth = VarNameToken.from(errors, toks);
		if (meth == null) {
			errors.message(toks, "missing method");
			return new IgnoreNestedParser(errors);
		}
		InputPosition lastLoc = meth.location;
		Tokenizable ec = toks.copyTo("->");
		List<Expr> args = new ArrayList<>();
		TDAExpressionParser expr = new TDAExpressionParser(errors, namer, x -> args.add(x), false, topLevel);
		expr.tryParsing(ec);
		if (errors.hasErrors()){
			return new IgnoreNestedParser(errors);
		}
		if (!args.isEmpty()) {
			lastLoc = args.get(args.size()-1).location();
		}
		Expr handler;
		if (ec != toks) {
			toks.reset(ec.at());
			ExprToken arrow = ExprToken.from(errors, toks);
			if (arrow == null) {
				errors.message(toks, "expected ->");
				return new IgnoreNestedParser(errors);
			}
			ExprToken name = ExprToken.from(errors, toks);
			if (name == null) {
				errors.message(toks, "expected var to store in");
				return new IgnoreNestedParser(errors);
			}
			if (name.type != ExprToken.IDENTIFIER) {
				errors.message(name.location, "expected var");
				return new IgnoreNestedParser(errors);
			}
			if (!name.text.startsWith("_")) {
				errors.message(name.location, "introduce vars must start with _");
				return new IgnoreNestedParser(errors);
			}
			lastLoc = name.location;
			IntroduceVar iv = new IntroduceVar(name.location, namer, name.text.substring(1), false);
			((IntroductionConsumer)topLevel).newIntroduction(errors, iv);
			handler = iv;
			errors.logReduction("unit-expect-introduce-handler", arrow, name);
			ih = "-introduce-handler";
		} else
			handler = new AnonymousVar(meth.location);
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		errors.logReduction("test-step-expect" + ih, kw.location, lastLoc);
		tellParent(kw.location);
		builder.expect(new UnresolvedVar(svc.location, svc.text), new UnresolvedVar(meth.location, meth.text), args.toArray(new Expr[args.size()]), handler);
		return new TDAParsingWithAction(
			new TDAMultiParser(errors),
			reduction(kw.location, "unit-test-expect")
		);
	}

	private TDAParsing handleExpectCancel(KeywordToken kw, Tokenizable toks) {
		ValidIdentifierToken handler = VarNameToken.from(errors, toks);
		if (handler == null) {
			errors.message(toks, "handler name required");
			return new IgnoreNestedParser(errors);
		}
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		errors.logReduction("unit-test-cancel", kw.location, handler.location);
		builder.expectCancel(new UnresolvedVar(handler.location, handler.text));
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleMatch(KeywordToken kw, Tokenizable toks) {
		ValidIdentifierToken card = VarNameToken.from(errors, toks);
		if (card == null) {
			errors.message(toks, "missing card");
			return new IgnoreNestedParser(errors);
		}
		KeywordToken whattok = KeywordToken.from(errors, toks);
		if (whattok == null) {
			errors.message(toks, "missing category");
			return new IgnoreNestedParser(errors);
		}
		MatchedItem what = new MatchedItem(whattok.text);
		/*
			errors.message(whattok.location, "cannot match '" + whattok.text + "'");
			return new IgnoreNestedParser(errors);
		*/
		
		tellParent(kw.location);
		TargetZone targetZoneTmp = new TargetZone(toks.realinfo(), new ArrayList<>());
		boolean containsTmp = false;
		boolean failsTmp = false;
		String withZone = "";
		String withType = "";
		InputPosition lastLoc = whattok.location;
		if (toks.hasMoreContent(errors)) {
			targetZoneTmp = parseTargetZone(toks);
			if (targetZoneTmp == null) {
				return new IgnoreNestedParser(errors);
			}
			lastLoc = targetZoneTmp.location;
			withZone = "-with-zone";
			KeywordToken option = KeywordToken.from(errors, toks);
			if (option != null) {
				lastLoc = option.location;
				withType = "-with-location-type";
				if ("contains".equals(option.text)) {
					containsTmp = true;
				} else if ("fails".equals(option.text)) {
					failsTmp = true;
				} else {
					errors.message(option.location, "invalid match type specification");
					return new IgnoreNestedParser(errors);
				}
			}
		}
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "unexpected characters at end of match");
			return new IgnoreNestedParser(errors);
		}
		final TargetZone targetZone = targetZoneTmp;
		final boolean contains = containsTmp;
		final boolean fails = failsTmp;
		errors.logReduction("unittest-match-command" + withZone + withType, kw.location, lastLoc);
		// TODO: should we return an expression parser for scroll matching?
		return new FreeTextParser(kw, errors, this, (lastPos, text) -> {
			reduce(kw.location, "unit-test-match");
			builder.match(new UnresolvedVar(card.location, card.text), what, targetZone, contains, fails, text);
		});
	}

	public TargetZone parseTargetZone(Tokenizable toks) {
		ArrayList<Object> tz = new ArrayList<>();
		int start = toks.at();
		InputPosition first = null, last = null, prevdot = null;
		boolean lastWasNumber = false;
		TriConsumer<String, InputPosition, InputPosition> reduce = (s,f,t) -> {}; 
		while (true) {
			EventZoneToken tok = EventZoneToken.from(errors, toks);
			if (tok == null) {
				errors.message(toks, "valid target zone expected");
				return null;
			} else if (tok.type == EventZoneToken.CARD) {
				if (tz.isEmpty()) {
					errors.logReduction("unit-test-target-zone", tok.location, tok.location().locAtEnd());
					return new TargetZone(tok.location, new ArrayList<>());
				}
				else {
					errors.message(tok.location, "valid target zone expected");
					return null;
				}
			} else if (tok.type == EventZoneToken.NAME) {
				if (prevdot != null) {
					reduce = (opt,from,to) -> errors.logReduction("uttz-field" + opt, from, to);
				}
				tz.add(tok.text);
				lastWasNumber = false;
			} else if (tok.type == EventZoneToken.NUMBER) {
				if (tz.isEmpty()) {
					errors.message(tok.location, "first entry in target cannot be list index");
					return null;
				} else if (lastWasNumber) {
					errors.message(tok.location, "cannot have consecutive list indices");
					return null;
				}
				if (prevdot != null) {
					reduce = (opt, from, to) -> errors.logReduction("uttz-index" + opt, from, to);
				}
				tz.add(Integer.parseInt(tok.text));
				lastWasNumber = true;
			} else {
				errors.message(tok.location, "valid target zone expected");
				return null;
			}
			if (first == null) {
				first = tok.location;
			}
			last = tok.location;
				
			if (!toks.hasMoreContent(errors)) {
				reduce.accept("", prevdot, tok.location);
				break;
			}
			
			int mark = toks.at();
			EventZoneToken dot = EventZoneToken.from(errors, toks);
			if (dot == null) {
				reduce.accept("", prevdot, tok.location);
				break;
			} else if (dot.type == EventZoneToken.DOT) {
				reduce.accept("", prevdot, tok.location);
				prevdot = dot.location;
				continue; // look for next symbol
			} else if (dot.type == EventZoneToken.COLON) {
				EventZoneToken qualifyingTemplate = EventZoneToken.from(errors, toks);
				if (qualifyingTemplate == null) {
					errors.message(dot.location, "target zone qualifier missing");
					return null;
				} else if (qualifyingTemplate.type != EventZoneToken.NAME) {
					errors.message(dot.location, "target zone qualifier must be a name");
					return null;
				}
				errors.logReduction("uttz-qualifier", dot.location, qualifyingTemplate.location);
				reduce.accept("-with-qualifier", prevdot, dot.location);
				last = dot.location;
				dot = EventZoneToken.from(errors, toks);
				if (dot == null)
					break;
				if (dot.type != EventZoneToken.DOT) {
					errors.message(dot.location, "target zone qualifier must be last item or followed by field");
					return null;
				}
				prevdot = dot.location;
				tz.add(new TargetZone.Qualifier(qualifyingTemplate.location, qualifyingTemplate.text));
			} else { // whatever it was, not what we want: assume the best and give it to the caller
				reduce.accept("", prevdot, tok.location);
				toks.reset(mark);
				break;
			}
		}
		if (tz.isEmpty()) {
			errors.message(toks, "valid target zone expected");
			return null;
		}
		if (tz.size() == 1 && "contains".equals(tz.get(0))) {
			toks.reset(start);
			return new TargetZone(first, new ArrayList<>());
		}
		errors.logReduction("unit-test-target-zone", first, last);
		return new TargetZone(first, tz);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
