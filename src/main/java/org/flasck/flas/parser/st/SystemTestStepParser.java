package org.flasck.flas.parser.st;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.ut.ConsumeDefinitions;
import org.flasck.flas.parser.ut.TestStepParser;
import org.flasck.flas.parser.ut.UnitDataNamer;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class SystemTestStepParser extends TestStepParser {

	public SystemTestStepParser(ErrorReporter errors, UnitDataNamer namer, SystemTestStage stage, TopLevelDefinitionConsumer topLevel, LocationTracker locTracker) {
		super(errors, namer, stage, new ConsumeDefinitions(errors, topLevel, null), locTracker); // null would have to be stage through an interface
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		int mark = toks.at();
		KeywordToken kw = KeywordToken.from(errors, toks);
		if (kw == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		updateLoc(kw.location);
		switch (kw.text) {
		case "assert": {
			return handleAssert(kw, toks);
		}
		case "contract": {
			return handleSendToContract(kw, toks);
		}
		case "data": {
			return handleDataDecl(kw, toks);
		}
		case "event": {
			return handleEvent(kw, toks);
		}
		case "input": {
			return handleInput(kw, toks);
		}
		case "match": {
			return handleMatch(kw, toks);
		}
		default: {
			toks.reset(mark);
			return null;
		}
		}
	}
}
