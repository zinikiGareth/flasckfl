package org.flasck.flas.parser.ut;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorMark;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.AnonymousVar;
import org.flasck.flas.parsedForm.TargetZone;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.ut.MatchedItem;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAExpressionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.stories.TDAMultiParser;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.TemplateNameToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.TypeNameToken;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class TestStepParser implements TDAParsing {
	private final ErrorReporter errors;
	private final UnitTestStepConsumer builder;
	private final UnitDataNamer namer;
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
			return new SingleExpressionParser(errors, ex -> { builder.assertion(test.get(0), ex); });
		}
		case "contract": {
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
			if (toks.hasMore()) {
				errors.message(toks, "garbage at end of line");
				return new IgnoreNestedParser();
			}
			builder.sendOnContract(new UnresolvedVar(tok.location, tok.text), new TypeReference(evname.location, evname.text), eventObj.get(0));
			return new NoNestingParser(errors);
		}
		case "data": {
			return new TDAUnitTestDataParser(errors, false, namer, dd -> { builder.data(dd); topLevel.nestedData(dd); }).tryParsing(toks);
		}
		case "event": {
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
			builder.event(new UnresolvedVar(tok.location, tok.text), targetZone, eventObj.get(0));
			return new NoNestingParser(errors);
		}
		case "invoke": {
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
			builder.invokeObjectMethod(eventObj.get(0));
			return new NoNestingParser(errors);
		}
		case "expect": {
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
			builder.expect(new UnresolvedVar(svc.location, svc.text), new UnresolvedVar(meth.location, meth.text), args.toArray(new Expr[args.size()]), handler);
			return new TDAMultiParser(errors);
		}
		case "match": {
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
			default:
				errors.message(whattok.location, "invalid category: " + whattok.text);
				return new IgnoreNestedParser();
			}
			
			TargetZone targetZone1 = new TargetZone(toks.realinfo(), "_");
			boolean contains1 = false;
			if (toks.hasMore()) {
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
			return new FreeTextParser(errors, text -> { builder.match(new UnresolvedVar(card.location, card.text), what, targetZone, contains, text); });
		}
		default: {
			toks.reset(mark);
			errors.message(toks, "unrecognized test step " + kw.text);
			return new IgnoreNestedParser();
		}
		}
	}

	private TargetZone parseTargetZone(Tokenizable toks) {
		// TODO: I think this may need to be a compound name to identify sub-elements
		InputPosition loc = toks.realinfo();
		int mk = toks.at();
		TemplateNameToken targetZone = TemplateNameToken.from(toks);
		if (targetZone == null) {
			if (toks.hasMore() && toks.nextChar() == '_') {
				toks.advance();
				if (!toks.hasMore() || Character.isWhitespace(toks.nextChar()))
					return new TargetZone(loc, "_");
				else {
					toks.reset(mk);
				}
			} else {
				errors.message(loc, "valid target zone expected");
				return null;
			}
		} else if (targetZone.text.equals("contains")) {
			toks.reset(mk);
			return new TargetZone(loc, "_");
		}
		return new TargetZone(targetZone.location, targetZone.text);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
