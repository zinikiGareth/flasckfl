package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.errors.ErrorMark;
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
		ErrorMark emark = errors.mark();
		boolean required = true;
		InputPosition optLoc = null;
		int mark = toks.at();
		KeywordToken ud = KeywordToken.from(errors, toks);
		if (ud != null) {
			if (ud.text.equals("optional")) {
				required = false;
				optLoc = ud.location;
			} else
				toks.reset(mark);
		}

		// Read the function name
		InputPosition pos = toks.realinfo();
		ValidIdentifierToken name = ValidIdentifierToken.from(errors, toks);
		if (name == null) {
			if (toks.hasMoreContent())
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
		TDAPatternParser pp = new TDAPatternParser(errors, new SimpleVarNamer(fnName), x-> args.add(x), topLevel);
		while (pp.tryParsing(toks) != null)
			;
		List<TypedPattern> targs = new ArrayList<>();
		for (Pattern p : args) {
			if (!(p instanceof TypedPattern))
				errors.message(p.location(), "contract patterns must be typed");
			else
				targs.add((TypedPattern) p);
		}
		if (emark.hasMoreNow())
			return new IgnoreNestedParser();
		TypedPattern handler = null;
		if (toks.hasMoreContent()) {
			// it must be a handler specification
			ExprToken tok = ExprToken.from(errors, toks);
			if (!"->".equals(tok.text)) {
				errors.message(tok.location, "syntax error");
				return new IgnoreNestedParser();
			}
			
			List<Pattern> hdlrs = new ArrayList<>();
			TDAPatternParser hp = new TDAPatternParser(errors, new SimpleVarNamer(fnName), x-> hdlrs.add(x), topLevel);
			hp.tryParsing(toks);
			if (hdlrs.size() != 1) {
				errors.message(toks, "no handler specified");
				return new IgnoreNestedParser();
			}
			Pattern p = hdlrs.get(0);
			if (!(p instanceof TypedPattern)) {
				errors.message(p.location(), "contract handler must be a typed pattern");
				return new IgnoreNestedParser();
			}
			handler = (TypedPattern) p;
		}
		if (toks.hasMoreContent()) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}

		ContractMethodDecl ret = new ContractMethodDecl(optLoc, name.location, name.location, required, fnName, targs, handler);
		builder.addMethod(ret);
		((ContractConsumer)topLevel).newContractMethod(errors, ret);
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
