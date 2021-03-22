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

	public TestStepParser(ErrorReporter errors, UnitDataNamer namer, UnitTestStepConsumer builder, UnitTestDefinitionConsumer topLevel) {
		this.errors = errors;
		this.namer = namer;
		this.builder = builder;
		this.topLevel = topLevel;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		int mark = toks.at();
		KeywordToken kw = KeywordToken.from(toks);
		if (kw == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		switch (kw.text) {
		case "assert": {
			return handleAssert(toks);
		}
		case "shove": {
			return handleShove(toks);
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
			return new IgnoreNestedParser();
		}
		}
	}

	protected TDAParsing handleAssert(Tokenizable toks) {
		List<Expr> test = new ArrayList<>();
		TDAExpressionParser expr = new TDAExpressionParser(errors, x -> test.add(x));
		expr.tryParsing(toks);
		if (errors.hasErrors()){
			return new IgnoreNestedParser();
		}
		if (test.isEmpty()) {
			errors.message(toks, "assert requires expression to evaluate");
			return new IgnoreNestedParser();
		}
		if (toks.hasMoreContent()) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		return new SingleExpressionParser(errors, "assert", ex -> { builder.assertion(test.get(0), ex); });
	}

	protected TDAParsing handleShove(Tokenizable toks) {
		List<UnresolvedVar> slots = new ArrayList<>();
		boolean haveDot = false;
		while (true) {
			ExprToken tok = ExprToken.from(errors, toks);
			if (tok == null || tok.type != ExprToken.IDENTIFIER) {
				errors.message(toks, "field path expected");
				return new IgnoreNestedParser();
			}
			UnresolvedVar v = new UnresolvedVar(tok.location, tok.text);
			slots.add(v);
			PuncToken dot = PuncToken.from(errors, toks);
			if (dot == null) {
				if (!haveDot) {
					errors.message(toks, ". expected");
					return new IgnoreNestedParser();
				}
				break;
			}
			if (".".equals(dot.text)) {
				haveDot = true;
			} else {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
		}

		if (toks.hasMoreContent()) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}

		return new SingleExpressionParser(errors, "shove", expr -> { builder.shove(slots, expr); });
	}

	protected TDAParsing handleSendToContract(Tokenizable toks) {
		ValidIdentifierToken tok = VarNameToken.from(toks);
		if (tok == null) {
			errors.message(toks, "contract requires a card variable to send the event to");
			return new IgnoreNestedParser();
		}
		TypeNameToken evname = TypeNameToken.qualified(toks);
		if (evname == null) {
			errors.message(toks, "contract requires a Contract name");
			return new IgnoreNestedParser();
		}
		List<Expr> eventObj = new ArrayList<>();
		TDAExpressionParser expr = new TDAExpressionParser(errors, x -> eventObj.add(x));
		expr.tryParsing(toks);
		if (errors.hasErrors()){
			return new IgnoreNestedParser();
		}
		if (eventObj.isEmpty()) {
			errors.message(toks, "missing arguments");
			return new IgnoreNestedParser();
		}
		if (toks.hasMoreContent()) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		builder.sendOnContract(new UnresolvedVar(tok.location, tok.text), new TypeReference(evname.location, evname.text), eventObj.get(0));
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleDataDecl(Tokenizable toks) {
		return new TDAUnitTestDataParser(errors, false, namer, dd -> { builder.data(dd); topLevel.nestedData(dd); }, topLevel).tryParsing(toks);
	}

	protected TDAParsing handleNewdiv(Tokenizable toks) {
		Integer cnt = null;
		if (toks.hasMoreContent()) {
			ExprToken tok = ExprToken.from(errors, toks);
			if (tok.type != ExprToken.NUMBER) {
				errors.message(toks, "integer required");
				return new IgnoreNestedParser();
			}
			try {
				cnt = Integer.parseInt(tok.text);
			} catch (NumberFormatException ex) {
				errors.message(toks, "integer required");
				return new IgnoreNestedParser();
			}
		}
		if (toks.hasMoreContent()) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		builder.newdiv(cnt);
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleRender(Tokenizable toks) {
		ValidIdentifierToken tok = VarNameToken.from(toks);
		if (tok == null) {
			errors.message(toks, "must specify a card to be rendered");
			return new IgnoreNestedParser();
		}
		ExprToken arrow = ExprToken.from(errors, toks);
		if (arrow == null || !"=>".equals(arrow.text)) {
			errors.message(toks, "=> expected");
			return new IgnoreNestedParser();
		}
		TemplateNameToken template = TemplateNameToken.from(errors, toks);
		if (template == null)
			return new IgnoreNestedParser();
		builder.render(new UnresolvedVar(tok.location, tok.text), new TemplateReference(template.location, namer.template(template.location, template.text)));
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleEvent(Tokenizable toks) {
		ValidIdentifierToken tok = VarNameToken.from(toks);
		if (tok == null) {
			errors.message(toks, "must specify a card to receive event");
			return new IgnoreNestedParser();
		}
		TargetZone targetZone = parseTargetZone(toks);
		if (targetZone == null) {
			return new IgnoreNestedParser();
		}
		ErrorMark em = errors.mark();
		List<Expr> eventObj = new ArrayList<>();
		TDAExpressionParser expr = new TDAExpressionParser(errors, x -> eventObj.add(x));
		expr.tryParsing(toks);
		if (em.hasMoreNow()){
			return new IgnoreNestedParser();
		}
		if (eventObj.isEmpty()) {
			errors.message(toks, "must provide an event object");
			return new IgnoreNestedParser();
		}
		if (eventObj.size() > 1) {
			errors.message(toks, "only one event object is allowed");
			return new IgnoreNestedParser();
		}
		if (toks.hasMoreContent()) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		builder.event(new UnresolvedVar(tok.location, tok.text), targetZone, eventObj.get(0));
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleInput(Tokenizable toks) {
		ValidIdentifierToken tok = VarNameToken.from(toks);
		if (tok == null) {
			errors.message(toks, "must specify a card to receive event");
			return new IgnoreNestedParser();
		}
		TargetZone targetZone = parseTargetZone(toks);
		if (targetZone == null) {
			return new IgnoreNestedParser();
		}
		if (toks.hasMoreContent()) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		return new SingleExpressionParser(errors, "input", text -> { builder.input(new UnresolvedVar(tok.location, tok.text), targetZone, text); });
	}

	protected TDAParsing handleInvoke(Tokenizable toks) {
		List<Expr> eventObj = new ArrayList<>();
		TDAExpressionParser expr = new TDAExpressionParser(errors, x -> eventObj.add(x));
		expr.tryParsing(toks);
		if (errors.hasErrors()){
			return new IgnoreNestedParser();
		}
		if (eventObj.isEmpty()) {
			errors.message(toks, "missing expression");
			return new IgnoreNestedParser();
		}
		if (toks.hasMoreContent()) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		builder.invokeObjectMethod(eventObj.get(0));
		return new NoNestingParser(errors);
	}

	protected TDAParsing handleExpect(Tokenizable toks) {
		ValidIdentifierToken svc = VarNameToken.from(toks);
		if (svc == null) {
			errors.message(toks, "missing contract");
			return new IgnoreNestedParser();
		}
		ValidIdentifierToken meth = VarNameToken.from(toks);
		if (meth == null) {
			errors.message(toks, "missing method");
			return new IgnoreNestedParser();
		}
		List<Expr> args = new ArrayList<>();
		TDAExpressionParser expr = new TDAExpressionParser(errors, namer, x -> args.add(x), false, topLevel);
		expr.tryParsing(toks);
		if (errors.hasErrors()){
			return new IgnoreNestedParser();
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
		if (toks.hasMoreContent()) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		builder.expect(new UnresolvedVar(svc.location, svc.text), new UnresolvedVar(meth.location, meth.text), args.toArray(new Expr[args.size()]), handler);
		return new TDAMultiParser(errors);
	}

	protected TDAParsing handleMatch(Tokenizable toks) {
		ValidIdentifierToken card = VarNameToken.from(toks);
		if (card == null) {
			errors.message(toks, "missing card");
			return new IgnoreNestedParser();
		}
		ValidIdentifierToken whattok = VarNameToken.from(toks);
		if (whattok == null) {
			errors.message(toks, "missing category");
			return new IgnoreNestedParser();
		}
		MatchedItem what;
		switch (whattok.text) {
		case "text":
			what = MatchedItem.TEXT;
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
		default:
			errors.message(whattok.location, "cannot match '" + whattok.text + "'");
			return new IgnoreNestedParser();
		}
		
		TargetZone targetZone1 = new TargetZone(toks.realinfo(), new ArrayList<>());
		boolean contains1 = false;
		if (toks.hasMoreContent()) {
			targetZone1 = parseTargetZone(toks);
			if (targetZone1 == null) {
				return new IgnoreNestedParser();
			}
			ValidIdentifierToken isContains = VarNameToken.from(toks);
			if (isContains != null && !"contains".equals(isContains.text)) {
				errors.message(isContains.location, "syntax error");
				return new IgnoreNestedParser();
			}
			contains1 = isContains != null;
		}
		TargetZone targetZone = targetZone1;
		boolean contains = contains1;
		if (toks.hasMoreContent()) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		// TODO: should we return an expression parser for scroll matching?
		return new FreeTextParser(errors, text -> { builder.match(new UnresolvedVar(card.location, card.text), what, targetZone, contains, text); });
	}

	public TargetZone parseTargetZone(Tokenizable toks) {
		ArrayList<Object> tz = new ArrayList<>();
		int start = toks.at();
		InputPosition first = null;
		boolean lastWasNumber = false;
		while (true) {
			EventZoneToken tok = EventZoneToken.from(toks);
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
				
			if (toks.hasMoreContent()) {
				int mark = toks.at();
				EventZoneToken dot = EventZoneToken.from(toks);
				if (dot == null)
					break;
				else if (dot.type != EventZoneToken.DOT) {
					toks.reset(mark);
					break;
				}
			} else
				break;
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
	}
}
