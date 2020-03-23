package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Pattern;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ContractDecl.ContractType;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public class ContractMethodParser implements TDAParsing {
	private final ErrorReporter errors;
	private final ContractMethodConsumer builder;
	private final FunctionScopeUnitConsumer topLevel;
	private final SolidName cname;
	private final ContractMethodDir dir;

	public ContractMethodParser(ErrorReporter errors, ContractMethodConsumer builder, FunctionScopeUnitConsumer topLevel, SolidName cname) {
		this.errors = errors;
		this.builder = builder;
		this.topLevel = topLevel;
		this.cname = cname;
		if (builder instanceof ContractDecl)
			dir = ((ContractDecl) builder).type == ContractType.SERVICE ? ContractMethodDir.UP : ContractMethodDir.DOWN;
		else
			dir = ContractMethodDir.DOWN;
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
		for (Pattern p : args) {
			if (!(p instanceof TypedPattern))
				errors.message(toks, "contract patterns must be typed");
		}
		if (errors.hasErrors())
			return new IgnoreNestedParser();

		builder.addMethod(new ContractMethodDecl(optLoc, name.location, name.location, required, dir, fnName, args));
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
