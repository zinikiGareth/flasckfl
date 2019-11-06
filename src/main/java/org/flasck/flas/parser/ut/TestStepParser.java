package org.flasck.flas.parser.ut;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAExpressionParser;
import org.flasck.flas.parser.TDAParsing;
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
		case "data": {
			return new TDAUnitTestDataParser(errors, true, namer, dd -> { builder.data(dd); topLevel.nestedData(dd); }).tryParsing(toks);
		}
		case "event": {
			ValidIdentifierToken tok = VarNameToken.from(toks);
			TemplateNameToken evname = TemplateNameToken.from(toks);
			List<Expr> eventObj = new ArrayList<>();
			TDAExpressionParser expr = new TDAExpressionParser(errors, x -> eventObj.add(x));
			expr.tryParsing(toks);
			if (errors.hasErrors()){
				return new IgnoreNestedParser();
			}
			if (tok == null || evname == null || eventObj.isEmpty()) {
				errors.message(toks, "missing arguments");
				return new IgnoreNestedParser();
			}
			builder.event(new UnresolvedVar(tok.location, tok.text), new StringLiteral(evname.location, evname.text), eventObj.get(0));
			return new NoNestingParser(errors);
		}
		case "send": {
			ValidIdentifierToken tok = VarNameToken.from(toks);
			TypeNameToken evname = TypeNameToken.qualified(toks);
			List<Expr> eventObj = new ArrayList<>();
			TDAExpressionParser expr = new TDAExpressionParser(errors, x -> eventObj.add(x));
			expr.tryParsing(toks);
			if (errors.hasErrors()){
				return new IgnoreNestedParser();
			}
			if (tok == null || evname == null || eventObj.isEmpty()) {
				errors.message(toks, "missing arguments");
				return new IgnoreNestedParser();
			}
			builder.send(new UnresolvedVar(tok.location, tok.text), new TypeReference(evname.location, evname.text), eventObj.get(0));
			return new NoNestingParser(errors);
		}
		case "template": {
			builder.template();
			return new NoNestingParser(errors);
		}
		default: {
			toks.reset(mark);
			errors.message(toks, "unrecognized test step " + kw.text);
			return new IgnoreNestedParser();
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
