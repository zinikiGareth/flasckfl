package org.flasck.flas.parser.ut;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDAUnitTestParser implements TDAParsing {
	private final ErrorReporter errors;
	private final UnitTestNamer namer;
	private final UnitTestDefinitionConsumer builder;

	public TDAUnitTestParser(ErrorReporter errors, UnitTestNamer namer, UnitTestDefinitionConsumer builder) {
		this.errors = errors;
		this.namer = namer;
		this.builder = builder;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken tok = KeywordToken.from(toks);
		if (tok == null || !"test".equals(tok.text)) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		final String desc = toks.remainder().trim();
		if (desc.length() == 0) {
			errors.message(toks, "test case must have a description");
			return new IgnoreNestedParser();
		}
		final UnitTestCase utc = new UnitTestCase(namer.unitTest(), desc);
		builder.testCase(utc);
		return new TestStepParser(errors, namer, utc);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
