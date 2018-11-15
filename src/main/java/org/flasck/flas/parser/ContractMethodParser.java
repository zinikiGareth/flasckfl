package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class ContractMethodParser implements TDAParsing {
	private final ErrorReporter errors;

	public ContractMethodParser(ErrorReporter errors, ContractDecl decl) {
		this.errors = errors;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken ud = KeywordToken.from(toks);
		if (ud == null || (!ud.text.equals("up") && !ud.text.equals("down"))) {
			errors.message(toks, "missing or invalid direction");
			return new IgnoreNestedParser();
		}
		return new NoNestingParser(errors);
	}

}
