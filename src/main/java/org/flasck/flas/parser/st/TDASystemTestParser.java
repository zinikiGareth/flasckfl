package org.flasck.flas.parser.st;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.SystemTestName;
import org.flasck.flas.compiler.ParsingPhase;
import org.flasck.flas.compiler.modules.ParserModule;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.st.SystemTestCleanup;
import org.flasck.flas.parsedForm.st.SystemTestConfiguration;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parser.BlockLocationTracker;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.st.TDASystemTestParser.OptionsRecorder;
import org.flasck.flas.parser.ut.TestStepNamer;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.TestDescriptionToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDASystemTestParser extends BlockLocationTracker implements TDAParsing {
	public static class OptionsRecorder {
		private boolean configure;
		private boolean finallyBlock;
		private Locatable first;

		public void haveConfigure(Locatable kw) {
			this.configure = true;
			from(kw);
		}

		public void haveTest(Locatable kw) {
			from(kw);
		}

		public void haveFinally(Locatable kw) {
			this.finallyBlock = true;
			from(kw);
		}
		
		private void from(Locatable kw) {
			if (first == null)
				first = kw;
		}
		
		public Locatable firstLoc() {
			return first;
		}
		
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("system-test");
			if (configure)
				sb.append("-configure");
			if (finallyBlock)
				sb.append("-finally");
			return sb.toString();
		}
	}

	private final SystemTestNamer namer;
	private final SystemTestDefinitionConsumer builder;
	private final TopLevelDefinitionConsumer topLevel;
	private final Iterable<ParserModule> modules;
	private final OptionsRecorder options;

	public TDASystemTestParser(ErrorReporter errors, SystemTestNamer namer, SystemTestDefinitionConsumer builder, TopLevelDefinitionConsumer topLevel, Iterable<ParserModule> modules, LocationTracker locTracker, OptionsRecorder options) {
		super(errors, locTracker);
		this.namer = namer;
		this.builder = builder;
		this.topLevel = topLevel;
		this.modules = modules;
		this.options = options;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		int mark = toks.at();
		KeywordToken tok = KeywordToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		updateLoc(tok.location);
		switch (tok.text) {
		case "configure": {
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "configure does not have a description");
				return new IgnoreNestedParser(errors);
			}
			options.haveConfigure(tok);
			SystemTestName stn = namer.special("configure");
			final SystemTestConfiguration stg = new SystemTestConfiguration(stn, topLevel);
			builder.configure(stg);
			errors.logReduction("system-test-stage-configure", tok.location, tok.location.locAtEnd());
			return new TDAParsingWithAction(
				ParsingPhase.systemTestStep(errors, new TestStepNamer(stn.container()), stg, topLevel, modules, this),
				reduction(tok.location, "system-test-configure")
			);
		}
		case "test": {
			toks.skipWS(errors);
			InputPosition pos = toks.realinfo();
			final String desc = toks.remainder().trim();
			errors.logParsingToken(new TestDescriptionToken(pos, desc));
			if (desc.length() == 0) {
				errors.message(toks, "each test step must have a description");
				return new IgnoreNestedParser(errors);
			}
			options.haveTest(tok);
			SystemTestName stn = namer.nextStep();
			final SystemTestStage stage = new SystemTestStage(stn, desc, topLevel);
			builder.test(stage);
			errors.logReduction("system-test-stage-test", tok.location, pos);
			return new TDAParsingWithAction(
				ParsingPhase.systemTestStep(errors, new TestStepNamer(stn), stage, topLevel, modules, this),
				reduction(tok.location, "system-test-unit")
			);
		}
		case "finally": {
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "finally does not have a description");
				return new IgnoreNestedParser(errors);
			}
			options.haveFinally(tok);
			SystemTestName stn = namer.special("finally");
			final SystemTestCleanup stg = new SystemTestCleanup(stn, topLevel);
			builder.cleanup(stg);
			errors.logReduction("system-test-stage-finally", tok.location, tok.location.locAtEnd());
			return new TDAParsingWithAction(
				ParsingPhase.systemTestStep(errors, new TestStepNamer(stn), stg, topLevel, modules, this),
				reduction(tok.location, "system-test-finally")
			);
		}
		default: {
			toks.reset(mark);
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		}
	}
		
	@Override
	public void scopeComplete(InputPosition location) {
	}
}
