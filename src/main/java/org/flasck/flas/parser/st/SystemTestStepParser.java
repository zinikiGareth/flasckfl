package org.flasck.flas.parser.st;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.StringLiteral;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.IntroduceVar;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.st.AjaxCreate;
import org.flasck.flas.parsedForm.st.AjaxPump;
import org.flasck.flas.parsedForm.st.MockApplication;
import org.flasck.flas.parsedForm.st.SystemTestStage;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAExpressionParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.parser.TopLevelDefinitionConsumer;
import org.flasck.flas.parser.ut.ConsumeDefinitions;
import org.flasck.flas.parser.ut.IntroductionConsumer;
import org.flasck.flas.parser.ut.TestStepParser;
import org.flasck.flas.parser.ut.UnitDataNamer;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.StringToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;
import org.flasck.flas.tokenizers.VarNameToken;

public class SystemTestStepParser extends TestStepParser {
	private final TopLevelDefinitionConsumer topLevel;

	public SystemTestStepParser(ErrorReporter errors, UnitDataNamer namer, SystemTestStage stage, TopLevelDefinitionConsumer topLevel) {
		super(errors, namer, stage, new ConsumeDefinitions(errors, topLevel, null)); // null would have to be stage through an interface
		this.topLevel = topLevel;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		int mark = toks.at();
		KeywordToken kw = KeywordToken.from(errors, toks);
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
		case "event": {
			return handleEvent(toks);
		}
		case "input": {
			return handleInput(toks);
		}
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
		case "application": {
			return handleApplication(toks);
		}
		case "route": {
			return handleRoute(toks);
		}
		case "login": {
			return handleLogin(toks);
		}
		default: {
			toks.reset(mark);
			return null;
		}
		}
	}

	private TDAParsing handleAjax(Tokenizable toks) {
		KeywordToken op = KeywordToken.from(errors, toks);
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
			((SystemTestStage)builder).ajaxCreate(errors, ac);
			return new AjaxCreateActionsParser(errors, ac);
		}
		case "pump": {
			ValidIdentifierToken tok = VarNameToken.from(toks);
			if (tok == null) {
				errors.message(toks, "no mock specified");
				return new IgnoreNestedParser();
			}
			if (toks.hasMoreContent()) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			VarName vn = namer.nameVar(tok.location, tok.text);
			AjaxPump pump = new AjaxPump(op.location, vn);
			((SystemTestStage)builder).ajaxPump(errors, pump);
			return new NoNestingParser(errors);
		}
		default: {
			errors.message(op.location, "unrecognized ajax operator: " + op.text);
			return new IgnoreNestedParser();
		}
		}
	}

	private TDAParsing handleApplication(Tokenizable toks) {
		ValidIdentifierToken tok = VarNameToken.from(toks);
		if (tok == null) {
			errors.message(toks, "no application name provided");
			return new IgnoreNestedParser();
		}
		if (toks.hasMoreContent()) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		VarName vn = namer.nameVar(tok.location, tok.text);
		// TODO: this is the thing that we can configure using the (hypothetical) nested parser
		PackageName pn = vn.packageName();
		MockApplication ma = new MockApplication(vn, pn);
		topLevel.mockApplication(errors, ma);
		((SystemTestStage)builder).mockApplication(errors, vn, ma);

		// TODO: theoretically, it should be possible to have a nested parser
		// this would allow you to say "package <name>" rather than the default
		return new NoNestingParser(errors);
	}

	private TDAParsing handleRoute(Tokenizable toks) {
		ValidIdentifierToken tok = VarNameToken.from(toks);
		if (tok == null) {
			errors.message(toks, "no application name provided");
			return new IgnoreNestedParser();
		}
		UnresolvedVar app = new UnresolvedVar(tok.location, tok.text);
		Tokenizable ec;
		int k = toks.find("->");
		if (k == -1) {
			ec = toks;
		} else
			ec = toks.cropAt(k);
		List<Expr> expr = new ArrayList<>();
		new TDAExpressionParser(errors, e -> {
			expr.add(e);
		}).tryParsing(ec);
		
		if (expr.size() != 1) {
			errors.message(toks, "expression expected");
			return new NoNestingParser(errors);
		}

		Expr route = expr.get(0);
		IntroduceVar iv = null;
		if (k != -1) {
			toks.reset(k);
			ExprToken arrow = ExprToken.from(errors, toks);
			if (arrow == null) {
				errors.message(toks, "expected ->");
				return new NoNestingParser(errors);
			}
			ExprToken name = ExprToken.from(errors, toks);
			if (name == null) {
				errors.message(toks, "expected var to store in");
				return new NoNestingParser(errors);
			}
			if (name.type != ExprToken.IDENTIFIER) {
				errors.message(name.location, "expected var");
				return new NoNestingParser(errors);
			}
			if (!name.text.startsWith("_")) {
				errors.message(name.location, "introduce vars must start with _");
				return new NoNestingParser(errors);
			}
			iv = new IntroduceVar(name.location, namer, name.text.substring(1));
			((IntroductionConsumer)builder).newIntroduction(errors, iv);
		}
		((SystemTestStage)builder).gotoRoute(errors, app, route, iv);

		if (toks.hasMoreContent()) {
			errors.message(toks, "junk at end of line");
		}
		return new NoNestingParser(errors);
	}

	private TDAParsing handleLogin(Tokenizable toks) {
		ValidIdentifierToken tok = VarNameToken.from(toks);
		if (tok == null) {
			errors.message(toks, "no application name provided");
			return new IgnoreNestedParser();
		}
		UnresolvedVar app = new UnresolvedVar(tok.location, tok.text);
		List<Expr> expr = new ArrayList<>();
		new TDAExpressionParser(errors, e -> {
			expr.add(e);
		}).tryParsing(toks);
		
		if (expr.size() != 1) {
			errors.message(toks, "expression expected");
			return new NoNestingParser(errors);
		}

		Expr user = expr.get(0);
		((SystemTestStage)builder).userLogin(errors, app, user);

		if (toks.hasMoreContent()) {
			errors.message(toks, "junk at end of line");
		}
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
