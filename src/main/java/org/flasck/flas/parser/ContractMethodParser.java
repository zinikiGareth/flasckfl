package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public class ContractMethodParser implements TDAParsing {
	private final ErrorReporter errors;
	private final ContractMethodConsumer builder;
	private final FunctionScopeUnitConsumer topLevel;

	public ContractMethodParser(ErrorReporter errors, ContractMethodConsumer builder, FunctionScopeUnitConsumer topLevel) {
		this.errors = errors;
		this.builder = builder;
		this.topLevel = topLevel;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken ud = KeywordToken.from(toks);
		if (ud == null) {
			errors.message(toks, "missing or invalid direction");
			return new IgnoreNestedParser();
		}
		boolean required = true;
		InputPosition optLoc = null;
		if (ud.text.equals("optional")) {
			required = false;
			optLoc = ud.location;
			ud = KeywordToken.from(toks);
		}
		if (ud == null || (!ud.text.equals("up") && !ud.text.equals("down"))) {
			errors.message(toks, "missing or invalid direction");
			return new IgnoreNestedParser();
		}

		// Read the function name
		ValidIdentifierToken name = ValidIdentifierToken.from(toks);
		if (name == null || Character.isUpperCase(name.text.charAt(0))) {
			// TOOD: surely this should generate an error? 
			return null;
		}
		FunctionName fnName = FunctionName.contractDecl(name.location, null, name.text);

		List<Object> args = new ArrayList<>();
		TDAPatternParser pp = new TDAPatternParser(errors, (loc, v) -> new VarName(loc, fnName, v), x-> args.add(x), topLevel);
		while (pp.tryParsing(toks) != null)
			;
		if (errors.hasErrors())
			return new IgnoreNestedParser();

		builder.addMethod(new ContractMethodDecl(optLoc, ud.location, name.location, required, ContractMethodDir.valueOf(ud.text.toUpperCase()), fnName, args));
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
