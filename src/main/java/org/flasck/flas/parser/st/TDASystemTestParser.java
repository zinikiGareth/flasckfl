package org.flasck.flas.parser.st;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SystemTestName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.st.SystemTestCleanup;
import org.flasck.flas.parsedForm.st.SystemTestConfiguration;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.ut.TestStepNamer;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDASystemTestParser implements TDAParsing {
	private final ErrorReporter errors;
	private final SystemTestNamer namer;
	private final SystemTestDefinitionConsumer builder;
	private final TopLevelDefinitionConsumer topLevel;

	public TDASystemTestParser(ErrorReporter errors, SystemTestNamer namer, SystemTestDefinitionConsumer builder, TopLevelDefinitionConsumer topLevel) {
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
			if (toks.hasMore()) {
				errors.message(toks, "configure does not have a description");
				return new IgnoreNestedParser();
			}
			SystemTestName stn = namer.special("configure");
			final SystemTestConfiguration stg = new SystemTestConfiguration(stn);
			builder.configure(stg);
			return new SystemTestStepParser(errors, new TestStepNamer(stn.container()), stg, topLevel);
		}
		case "test": {
			final String desc = toks.remainder().trim();
			if (desc.length() == 0) {
				errors.message(toks, "each test step must have a description");
				return new IgnoreNestedParser();
			}
			SystemTestName stn = namer.nextStep();
			final SystemTestStage stage = new SystemTestStage(stn, desc);
			builder.test(stage);
			return new SystemTestStepParser(errors, new TestStepNamer(stn), stage, topLevel);
		}
		case "finally": {
			if (toks.hasMore()) {
				errors.message(toks, "finally does not have a description");
				return new IgnoreNestedParser();
			}
			SystemTestName stn = namer.special("finally");
			final SystemTestCleanup stg = new SystemTestCleanup(stn);
			builder.cleanup(stg);
			return new SystemTestStepParser(errors, new TestStepNamer(stn), stg, topLevel);
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
