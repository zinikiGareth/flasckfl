package org.flasck.flas.parser;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractMethodDecl;
import org.flasck.flas.parsedForm.ContractMethodDir;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;
import org.flasck.flas.tokenizers.ValidIdentifierToken;

public class ContractMethodParser implements TDAParsing {
	private final ErrorReporter errors;
	private final ContractMethodConsumer builder;

	public ContractMethodParser(ErrorReporter errors, ContractMethodConsumer builder) {
		this.errors = errors;
		this.builder = builder;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken ud = KeywordToken.from(toks);
		if (ud == null || (!ud.text.equals("up") && !ud.text.equals("down"))) {
			errors.message(toks, "missing or invalid direction");
			return new IgnoreNestedParser();
		}

		// Read the function name
		ValidIdentifierToken name = ValidIdentifierToken.from(toks);
		if (name == null || Character.isUpperCase(name.text.charAt(0)))
			return null;

		List<Object> args = new ArrayList<>();
		FunctionName fnName = FunctionName.contractDecl(name.location, null, name.text);
		builder.addMethod(new ContractMethodDecl(null, ud.location, name.location, true, ContractMethodDir.UP, fnName , args));

		return new NoNestingParser(errors);
	}

}
