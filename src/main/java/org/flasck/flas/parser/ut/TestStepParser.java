package org.flasck.flas.parser.ut;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parsedForm.TemplateReference;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.MatchedItem;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
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

public class TestStepParser implements TDAParsing {
	protected final ErrorReporter errors;
	protected final UnitTestStepConsumer builder;
	protected final UnitDataNamer namer;
	private final UnitTestDefinitionConsumer topLevel;
	private final String mainRule;
	private final InputPosition parentLocation;

	public TestStepParser(ErrorReporter errors, UnitDataNamer namer, UnitTestStepConsumer builder, UnitTestDefinitionConsumer topLevel, String mainRule, InputPosition parentLocation) {
		this.errors = errors;
		this.namer = namer;
		this.builder = builder;
		this.topLevel = topLevel;
		this.mainRule = mainRule;
		this.parentLocation = parentLocation;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		int mark = toks.at();
		KeywordToken kw = KeywordToken.from(errors, toks);
		if (kw == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		switch (kw.text) {
		case "assert": {
			return handleAssert(kw, toks);
		}
		case "identical": {
			return handleIdentical(toks);
		}
		case "shove": {
			return handleShove(toks);
		}
		case "close": {
			return closeCard(toks);
		}
		case "contract": {
			return handleSendToContract(toks);
		}
		case "data": {
			return handleDataDecl(toks);
		}
		case "newdiv":
			return handleNewdiv(toks);
		case "render": {
			return handleRender(toks);
		}
		case "event": {
			return handleEvent(toks);
		}
		case "input": {
			return handleInput(toks);
		}
		case "invoke": {
			return handleInvoke(toks);
		}
		case "expect": {
			return handleExpect(toks);
		}
		case "match": {
			return handleMatch(toks);
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
		TDAExpressionParser expr = new TDAExpressionParser(errors, x -> test.add(x));
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
		return new SingleExpressionParser(errors, "assert", ex -> { errors.logReduction("ut-assert", kw, ex); builder.assertion(test.get(0), ex); });
	}

	protected TDAParsing handleIdentical(Tokenizable toks) {
		List<Expr> test = new ArrayList<>();
		TDAExpressionParser expr = new TDAExpressionParser(errors, x -> test.add(x));
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
		return new SingleExpressionParser(errors, "identical", ex -> { builder.identical(test.get(0), ex); });
	}

	protected TDAParsing handleShove(Tokenizable toks) {
		List<UnresolvedVar> slots = new ArrayList<>();
		boolean haveDot = false;
		while (true) {
			ExprToken tok = ExprToken.from(errors, toks);
			if (tok == null || tok.type != ExprToken.IDENTIFIER) {
				errors.message(toks, "field path expected");
				return new IgnoreNestedParser(errors);
			}
			UnresolvedVar v = new UnresolvedVar(tok.location, tok.text);
			slots.add(v);
			PuncToken dot = PuncToken.from(errors, toks);
			if (dot == null) {
				if (!haveDot) {
					errors.message(toks, ". expected");
					return new IgnoreNestedParser(errors);
				}
				break;
			}
			if (".".equals(dot.text)) {
				haveDot = true;
			} else {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser(errors);
			}
		}

		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}

		return new SingleExpressionParser(errors, "shove", expr -> { builder.shove(slots, expr); });
	}

	protected TDAParsing handleSendToContract(Tokenizable toks) {
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
		List<Expr> eventObj = new ArrayList<>();
		TDAExpressionParser expr = new TDAExpressionParser(errors, x -> eventObj.add(x));
		expr.tryParsing(toks);
		if (errors.hasErrors()){
			return new IgnoreNestedParser(errors);
		}
		if (eventObj.isEmpty()) {
			errors.message(toks, "missing arguments");
			return new IgnoreNestedParser(errors);
		}
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		builder.sendOnContract(new UnresolvedVar(tok.location, tok.text), new TypeReference(evname.location, evname.text), eventObj.get(0));
		return new NoNestingParser(errors);
	}

	protected TDAParsing closeCard(Tokenizable toks) {
		ValidIdentifierToken tok = VarNameToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "close requires a card variable to close");
			return new IgnoreNestedParser(errors);
		}
		builder.closeCard(new UnresolvedVar(tok.location, tok.text));
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleDataDecl(Tokenizable toks) {
		return new TDAUnitTestDataParser(errors, false, namer, dd -> { builder.data(errors, dd); topLevel.nestedData(dd); }, topLevel).tryParsing(toks);
	}

	protected TDAParsing handleNewdiv(Tokenizable toks) {
		Integer cnt = null;
		if (toks.hasMoreContent(errors)) {
			ExprToken tok = ExprToken.from(errors, toks);
			if (tok.type != ExprToken.NUMBER) {
				errors.message(toks, "integer required");
				return new IgnoreNestedParser(errors);
			}
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
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleRender(Tokenizable toks) {
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
		builder.render(new UnresolvedVar(tok.location, tok.text), new TemplateReference(template.location, namer.template(template.location, template.text)));
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleEvent(Tokenizable toks) {
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
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleInput(Tokenizable toks) {
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
		return new SingleExpressionParser(errors, "input", text -> { builder.input(new UnresolvedVar(tok.location, tok.text), targetZone, text); });
	}

	protected TDAParsing handleInvoke(Tokenizable toks) {
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
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		builder.invokeObjectMethod(eventObj.get(0));
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleExpect(Tokenizable toks) {
		ValidIdentifierToken svc = VarNameToken.from(errors, toks);
		if (svc == null) {
			// If we don't have a service name, then it could be a contract.
			// Look for the special operator "<x"
			ExprToken tok = ExprToken.from(errors, toks);
			if (tok != null && tok.type == ExprToken.SYMBOL) {
				if (tok.text.equals("<~"))
					return handleExpectCancel(toks);
				errors.message(toks, "invalid expect operator " + tok.text);
				return new IgnoreNestedParser(errors);
			}
			errors.message(toks, "missing contract");
			return new IgnoreNestedParser(errors);
		}
		ValidIdentifierToken meth = VarNameToken.from(errors, toks);
		if (meth == null) {
			errors.message(toks, "missing method");
			return new IgnoreNestedParser(errors);
		}
		List<Expr> args = new ArrayList<>();
		TDAExpressionParser expr = new TDAExpressionParser(errors, namer, x -> args.add(x), false, topLevel);
		expr.tryParsing(toks);
		if (errors.hasErrors()){
			return new IgnoreNestedParser(errors);
		}
		Expr handler = null;
		if (args.size() >= 2) {
			Expr op = args.get(args.size()-2);
			if (op instanceof UnresolvedOperator && ((UnresolvedOperator)op).op.equals("->")) {
				args.remove(args.size()-2);
				handler = args.remove(args.size()-1);
			}
		}
		if (handler == null)
			handler = new AnonymousVar(meth.location);
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		builder.expect(new UnresolvedVar(svc.location, svc.text), new UnresolvedVar(meth.location, meth.text), args.toArray(new Expr[args.size()]), handler);
		return new TDAMultiParser(errors);
	}

	private TDAParsing handleExpectCancel(Tokenizable toks) {
		ValidIdentifierToken handler = VarNameToken.from(errors, toks);
		if (handler == null) {
			errors.message(toks, "handler name required");
			return new IgnoreNestedParser(errors);
		}
		if (toks.hasMoreContent(errors)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		builder.expectCancel(new UnresolvedVar(handler.location, handler.text));
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleMatch(Tokenizable toks) {
		ValidIdentifierToken card = VarNameToken.from(errors, toks);
		if (card == null) {
			errors.message(toks, "missing card");
			return new IgnoreNestedParser(errors);
		}
		ValidIdentifierToken whattok = VarNameToken.from(errors, toks);
		if (whattok == null) {
			errors.message(toks, "missing category");
			return new IgnoreNestedParser(errors);
		}
		MatchedItem what;
		switch (whattok.text) {
		case "text":
			what = MatchedItem.TEXT;
			break;
		case "title":
			what = MatchedItem.TITLE;
			break;
		case "style":
			what = MatchedItem.STYLE;
			break;
		case "scroll":
			what = MatchedItem.SCROLL;
			break;
		case "image":
			what = MatchedItem.IMAGE_URI;
			break;
		case "href":
			what = MatchedItem.HREF;
			break;
		default:
			errors.message(whattok.location, "cannot match '" + whattok.text + "'");
			return new IgnoreNestedParser(errors);
		}
		
		TargetZone targetZoneTmp = new TargetZone(toks.realinfo(), new ArrayList<>());
		boolean containsTmp = false;
		boolean failsTmp = false;
		if (toks.hasMoreContent(errors)) {
			targetZoneTmp = parseTargetZone(toks);
			if (targetZoneTmp == null) {
				return new IgnoreNestedParser(errors);
			}
			ValidIdentifierToken option = VarNameToken.from(errors, toks);
			if (option != null) {
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
		// TODO: should we return an expression parser for scroll matching?
		return new FreeTextParser(errors, text -> { builder.match(new UnresolvedVar(card.location, card.text), what, targetZone, contains, fails, text); });
	}

	public TargetZone parseTargetZone(Tokenizable toks) {
		ArrayList<Object> tz = new ArrayList<>();
		int start = toks.at();
		InputPosition first = null;
		boolean lastWasNumber = false;
		while (true) {
			EventZoneToken tok = EventZoneToken.from(errors, toks);
			if (tok == null) {
				errors.message(toks, "valid target zone expected");
				return null;
			} else if (tok.type == EventZoneToken.CARD) {
				if (tz.isEmpty())
					return new TargetZone(tok.location, new ArrayList<>());
				else {
					errors.message(tok.location, "valid target zone expected");
					return null;
				}
			} else if (tok.type == EventZoneToken.NAME) {
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
				tz.add(Integer.parseInt(tok.text));
				lastWasNumber = true;
			} else {
				errors.message(tok.location, "valid target zone expected");
				return null;
			}
			if (first == null) {
				first = tok.location;
			}
				
			if (!toks.hasMoreContent(errors))
				break;
			
			int mark = toks.at();
			EventZoneToken dot = EventZoneToken.from(errors, toks);
			if (dot == null) {
				break;
			} else if (dot.type == EventZoneToken.DOT) {
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
				dot = EventZoneToken.from(errors, toks);
				if (dot == null)
					break;
				if (dot.type != EventZoneToken.DOT) {
					errors.message(dot.location, "target zone qualifier must be followed by field");
					return null;
				}
				tz.add(new TargetZone.Qualifier(qualifyingTemplate.location, qualifyingTemplate.text));
			} else { // whatever it was, not what we want: assume the best and give it to the caller
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
		return new TargetZone(first, tz);
	}

	@Override
	public void scopeComplete(InputPosition location) {
		errors.logReduction(mainRule, parentLocation, location);
	}
}
