package org.flasck.flas.parser.st;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.ut.ConsumeDefinitions;
import org.flasck.flas.parser.ut.TestStepParser;
import org.flasck.flas.parser.ut.UnitDataNamer;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class SystemTestStepParser extends TestStepParser {
	public SystemTestStepParser(ErrorReporter errors, UnitDataNamer namer, SystemTestStage stage, TopLevelDefinitionConsumer topLevel) {
		super(errors, namer, stage, new ConsumeDefinitions(errors, topLevel, null)); // null would have to be stage through an interface
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
			return handleAssert(toks);
		}
//		case "shove": {
//			return handleShove(toks);
//		}
		case "contract": {
			return handleSendToContract(toks);
		}
		case "data": {
			return handleDataDecl(toks);
		}
//		case "newdiv":
//			return handleNewdiv(toks);
//		case "render": {
//			return handleRender(toks);
//		}
//		case "event": {
//			return handleEvent(toks);
//		}
//		case "invoke": {
//			return handleInvoke(toks);
//		}
//		case "expect": {
//			return handleExpect(toks);
//		}
//		case "match": {
//			return handleMatch(toks);
//		}
		default: {
			toks.reset(mark);
			errors.message(toks, "unrecognized system test step " + kw.text);
			return new IgnoreNestedParser();
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
