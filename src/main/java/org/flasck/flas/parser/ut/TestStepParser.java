package org.flasck.flas.parser.ut;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAExpressionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelNamer;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

import test.parsing.ut.SingleExpressionParser;

public class TestStepParser implements TDAParsing {
	private final ErrorReporter errors;
	private final UnitTestStepConsumer builder;

	public TestStepParser(ErrorReporter errors, TopLevelNamer namer, UnitTestStepConsumer builder) {
		this.errors = errors;
		this.builder = builder;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
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
			if (toks.hasMore()){
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			return new SingleExpressionParser(errors, ex -> { builder.assertion(test.get(0), ex); });
		}
		default: {
			errors.message(toks, "unrecognized test step " + kw.text);
			return new IgnoreNestedParser();
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
