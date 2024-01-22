package org.flasck.flas.parser.st;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SystemTestName;
import org.flasck.flas.compiler.ParsingPhase;
import org.flasck.flas.compiler.modules.ParserModule;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.st.SystemTestCleanup;
import org.flasck.flas.parsedForm.st.SystemTestConfiguration;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.ut.TestStepNamer;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.TestDescriptionToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class TDASystemTestParser implements TDAParsing, LocationTracker {
	private final ErrorReporter errors;
	private final SystemTestNamer namer;
	private final SystemTestDefinitionConsumer builder;
	private final TopLevelDefinitionConsumer topLevel;
	private final Iterable<ParserModule> modules;
	private final LocationTracker locTracker;
	private Runnable onComplete;
	private InputPosition lastInner;

	public TDASystemTestParser(ErrorReporter errors, SystemTestNamer namer, SystemTestDefinitionConsumer builder, TopLevelDefinitionConsumer topLevel, Iterable<ParserModule> modules, LocationTracker locTracker) {
		this.errors = errors;
		this.namer = namer;
		this.builder = builder;
		this.topLevel = topLevel;
		this.modules = modules;
		this.locTracker = locTracker;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		if (onComplete != null) {
			onComplete.run();
			onComplete = null;
		}
		int mark = toks.at();
		KeywordToken tok = KeywordToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		lastInner = tok.location;
		switch (tok.text) {
		case "configure": {
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "configure does not have a description");
				return new IgnoreNestedParser(errors);
			}
			SystemTestName stn = namer.special("configure");
			final SystemTestConfiguration stg = new SystemTestConfiguration(stn, topLevel);
			builder.configure(stg);
			errors.logReduction("system-test-stage-configure", tok.location, tok.location);
			onComplete = () -> {
				errors.logReduction("system-test-stage-configure-with-steps", tok.location, lastInner);
				if (locTracker != null)
					locTracker.updateLoc(tok.location);
			};
			return ParsingPhase.systemTestStep(errors, new TestStepNamer(stn.container()), stg, topLevel, modules, this);
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
			SystemTestName stn = namer.nextStep();
			final SystemTestStage stage = new SystemTestStage(stn, desc, topLevel);
			builder.test(stage);
			errors.logReduction("system-test-stage-test", tok.location, pos);
			lastInner = pos;
			onComplete = () -> {
				errors.logReduction("system-test-stage-test-with-steps", tok.location, lastInner);
				if (locTracker != null)
					locTracker.updateLoc(tok.location);
			};
			return ParsingPhase.systemTestStep(errors, new TestStepNamer(stn), stage, topLevel, modules, this);
		}
		case "finally": {
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "finally does not have a description");
				return new IgnoreNestedParser(errors);
			}
			SystemTestName stn = namer.special("finally");
			final SystemTestCleanup stg = new SystemTestCleanup(stn, topLevel);
			builder.cleanup(stg);
			errors.logReduction("system-test-stage-finally", tok.location, tok.location);
			onComplete = () -> {
				errors.logReduction("system-test-stage-finally-with-steps", tok.location, lastInner);
				if (locTracker != null)
					locTracker.updateLoc(tok.location);
			};
			return ParsingPhase.systemTestStep(errors, new TestStepNamer(stn), stg, topLevel, modules, this);
		}
		default: {
			toks.reset(mark);
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		}
	}
		
	@Override
	public void updateLoc(InputPosition location) {
		if (location != null && (lastInner == null || location.compareTo(lastInner) > 0))
			lastInner = location;
	}

	@Override
	public void scopeComplete(InputPosition location) {
		if (onComplete != null) {
			onComplete.run();
			onComplete = null;
		}
	}
}
