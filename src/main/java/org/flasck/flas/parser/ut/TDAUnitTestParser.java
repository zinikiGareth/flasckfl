package org.flasck.flas.parser.ut;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAUnitTestParser implements TDAParsing {
	private final ErrorReporter errors;
	private final UnitTestNamer namer;
	private final UnitTestDefinitionConsumer builder;
	private final FunctionScopeUnitConsumer topLevel;

	public TDAUnitTestParser(ErrorReporter errors, UnitTestNamer namer, UnitTestDefinitionConsumer builder, FunctionScopeUnitConsumer topLevel) {
		this.errors = errors;
		this.namer = namer;
		this.builder = builder;
		this.topLevel = topLevel;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		int mark = toks.at();
		KeywordToken tok = KeywordToken.from(toks);
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		switch (tok.text) {
		case "data": {
			return new TDAUnitTestDataParser(errors, false, namer, dd -> builder.data(dd), topLevel).tryParsing(toks);
		}
		case "test": {
			final String desc = toks.remainder().trim();
			if (desc.length() == 0) {
				errors.message(toks, "test case must have a description");
				return new IgnoreNestedParser();
			}
			final UnitTestCase utc = new UnitTestCase(namer.unitTest(), desc);
			builder.testCase(utc);
			return new TestStepParser(errors, new TestStepNamer(utc.name), utc, builder);
		}
		case "ignore": {
			// Do what it says on the can ... ignore this line and all nested lines
			return new IgnoreNestedParser();
		}
		default: {
			toks.reset(mark);
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
