package org.flasck.flas.parser.ut;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ut.UnitTestCase;
import org.flasck.flas.parser.BlockLocationTracker;
import org.flasck.flas.parser.FunctionScopeUnitConsumer;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.TestDescriptionToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TDAUnitTestParser extends BlockLocationTracker implements TDAParsing {
	public static final Logger logger = LoggerFactory.getLogger("UTParser");
	private final UnitTestNamer namer;
	private final UnitTestDefinitionConsumer builder;
	private final FunctionScopeUnitConsumer topLevel;

	public TDAUnitTestParser(ErrorReporter errors, UnitTestNamer namer, UnitTestDefinitionConsumer builder, FunctionScopeUnitConsumer topLevel) {
		super(errors, null);
		this.namer = namer;
		this.builder = builder;
		this.topLevel = topLevel;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		int mark = toks.at();
		KeywordToken tok = KeywordToken.from(errors, toks);
		if (tok == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		switch (tok.text) {
		case "data": {
			return new TDAUnitTestDataParser(errors, false, tok, namer, dd -> builder.data(dd), topLevel, this).tryParsing(toks);
		}
		case "test": {
			toks.skipWS(errors);
			InputPosition pos = toks.realinfo();
			final String desc = toks.remainder().trim();
			TestDescriptionToken tdt = new TestDescriptionToken(pos, desc);
			errors.logParsingToken(tdt);
			if (desc.length() == 0) {
				errors.message(toks, "test case must have a description");
				return new IgnoreNestedParser(errors);
			}
			errors.logReduction("ut-test-intro", tok, tdt);
			updateLoc(tok.location);
			final UnitTestCase utc = new UnitTestCase(namer.unitTest(), desc);
			builder.testCase(utc);
			return new TDAParsingWithAction(
				new TestStepParser(errors, new TestStepNamer(utc.name), utc, builder, this),
				reduction(tok.location, "unit-test-declaration")
			);
		}
		case "ignore": {
			toks.skipWS(errors);
			InputPosition pos = toks.realinfo();
			final String desc = toks.remainder().trim();
			TestDescriptionToken tdt = new TestDescriptionToken(pos, desc);
			errors.logParsingToken(tdt);
			errors.logReduction("ut-ignore-test-intro", tok, tdt);
			updateLoc(tok.location);
			
			// Do what it says on the can ... ignore this line and all nested lines
			return new TDAParsingWithAction(
				new IgnoreNestedParser(errors, this),
				reduction(tok.location, "ut-ignore-test-with-steps")
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
