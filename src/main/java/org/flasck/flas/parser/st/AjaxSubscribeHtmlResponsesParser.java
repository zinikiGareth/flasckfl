package org.flasck.flas.parser.st;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.st.AjaxSubscribe;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.FreeTextToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class AjaxSubscribeHtmlResponsesParser implements TDAParsing {
	private final ErrorReporter errors;
	private final AjaxSubscribe sub;

	public AjaxSubscribeHtmlResponsesParser(ErrorReporter errors, AjaxSubscribe sub) {
		this.errors = errors;
		this.sub = sub;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		InputPosition loc = toks.realinfo();
		String text = toks.remainder();
		errors.logParsingToken(new FreeTextToken(loc, text));
		sub.html(loc, text);
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
