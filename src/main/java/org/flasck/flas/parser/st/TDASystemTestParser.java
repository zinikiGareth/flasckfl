package org.flasck.flas.parser.st;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDASystemTestParser implements TDAParsing {
	private final ErrorReporter errors;
	private final SystemTestNamer namer;
	private final SystemTestDefinitionConsumer builder;
	private final FunctionScopeUnitConsumer topLevel;

	public TDASystemTestParser(ErrorReporter errors, SystemTestNamer namer, SystemTestDefinitionConsumer builder, FunctionScopeUnitConsumer topLevel) {
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
		case "configure": {
//			return new TDAUnitTestDataParser(errors, false, namer, dd -> builder.data(dd), topLevel).tryParsing(toks);
		}
		case "test": {
			final String desc = toks.remainder().trim();
			if (desc.length() == 0) {
				errors.message(toks, "each test step must have a description");
				return new IgnoreNestedParser();
			}
			final SystemTestStage stage = new SystemTestStage(namer.nextStep(), desc);
			builder.test(stage);
			return new SystemTestStepParser(errors, /* new TestStepNamer(utc.name), */ stage /*, builder */);
		}
		case "cleanup": {
//			throw new NotImplementedException("cleanup");
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
