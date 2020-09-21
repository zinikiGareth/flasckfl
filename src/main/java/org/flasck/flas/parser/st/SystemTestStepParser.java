package org.flasck.flas.parser.st;

import java.net.URI;
import java.net.URISyntaxException;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.st.AjaxCreate;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.ut.ConsumeDefinitions;
import org.flasck.flas.parser.ut.TestStepParser;
import org.flasck.flas.parser.ut.UnitDataNamer;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

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
		case "match": {
			return handleMatch(toks);
		}
		case "ajax": {
			return handleAjax(toks);
		}
		default: {
			toks.reset(mark);
			errors.message(toks, "unrecognized system test step " + kw.text);
			return new IgnoreNestedParser();
		}
		}
	}

	private TDAParsing handleAjax(Tokenizable toks) {
		KeywordToken op = KeywordToken.from(toks);
		if (op == null) {
			errors.message(toks, "ajax command requires an operator");
			return new IgnoreNestedParser();
		}
		switch (op.text) {
		case "create": {
			ValidIdentifierToken tok = VarNameToken.from(toks);
			if (tok == null) {
				errors.message(toks, "ajax create requires a variable to name the mock");
				return new IgnoreNestedParser();
			}
			InputPosition loc = toks.realinfo();
			String sl = StringToken.from(errors, toks);
			if (sl == null) {
				errors.message(toks, "ajax create requires a base url");
				return new IgnoreNestedParser();
			}
			try {
				new URI(sl);
			} catch (URISyntaxException ex) {
				errors.message(toks, "invalid ajax uri: " + sl);
			}
			VarName vn = namer.nameVar(tok.location, tok.text);
			StringLiteral baseUrl = new StringLiteral(loc, sl);
			AjaxCreate ac = new AjaxCreate(op.location, vn, baseUrl);
			((SystemTestStage)builder).ajax(errors, ac);
			return new AjaxCreateActionsParser(errors, ac);
		}
		default: {
			errors.message(toks, "unrecognized ajax operator: " + op.text);
			return new IgnoreNestedParser();
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
