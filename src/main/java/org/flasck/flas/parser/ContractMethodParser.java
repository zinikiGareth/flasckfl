package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.tokenizers.ExprToken;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public class ContractMethodParser implements TDAParsing {
	private final ErrorReporter errors;
	private final ContractMethodConsumer builder;
	private final FunctionScopeUnitConsumer topLevel;
	private final SolidName cname;

	public ContractMethodParser(ErrorReporter errors, ContractMethodConsumer builder, FunctionScopeUnitConsumer topLevel, SolidName cname) {
		this.errors = errors;
		this.builder = builder;
		this.topLevel = topLevel;
		this.cname = cname;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		boolean required = true;
		InputPosition optLoc = null;
		int mark = toks.at();
		KeywordToken ud = KeywordToken.from(toks);
		if (ud != null) {
			if (ud.text.equals("optional")) {
				required = false;
				optLoc = ud.location;
			} else
				toks.reset(mark);
		}

		// Read the function name
		InputPosition pos = toks.realinfo();
		ValidIdentifierToken name = ValidIdentifierToken.from(toks);
		if (name == null) {
			if (toks.hasMore())
				errors.message(toks, "invalid method name");
			else
				errors.message(toks, "missing method name");
			return new NoNestingParser(errors);
		} else if (Character.isUpperCase(name.text.charAt(0))) {
			errors.message(pos, "invalid method name");
			return new NoNestingParser(errors);
		}
		FunctionName fnName = FunctionName.contractDecl(name.location, cname, name.text);

		List<Pattern> args = new ArrayList<>();
		TDAPatternParser pp = new TDAPatternParser(errors, (loc, v) -> new VarName(loc, fnName, v), x-> args.add(x), topLevel);
		while (pp.tryParsing(toks) != null)
			;
		List<TypedPattern> targs = new ArrayList<>();
		for (Pattern p : args) {
			if (!(p instanceof TypedPattern))
				errors.message(toks, "contract patterns must be typed");
			else
				targs.add((TypedPattern) p);
		}
		if (errors.hasErrors())
			return new IgnoreNestedParser();
		TypedPattern handler = null;
		if (toks.hasMore()) {
			// it must be a handler specification
			ExprToken tok = ExprToken.from(errors, toks);
			if (!"->".equals(tok.text)) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			
			List<Pattern> hdlrs = new ArrayList<>();
			TDAPatternParser hp = new TDAPatternParser(errors, (loc, v) -> new VarName(loc, fnName, v), x-> hdlrs.add(x), topLevel);
			hp.tryParsing(toks);
			if (hdlrs.size() != 1) {
				errors.message(toks, "no handler specified");
				return new IgnoreNestedParser();
			}
			if (!(hdlrs.get(0) instanceof TypedPattern))
				errors.message(toks, "contract handler must be typed");
			handler = (TypedPattern) hdlrs.get(0);
		}
		if (toks.hasMore()) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}

		builder.addMethod(new ContractMethodDecl(optLoc, name.location, name.location, required, fnName, targs, handler));
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
