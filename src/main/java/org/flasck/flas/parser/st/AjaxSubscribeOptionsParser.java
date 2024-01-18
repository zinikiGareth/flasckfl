package org.flasck.flas.parser.st;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.st.AjaxSubscribe;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class AjaxSubscribeOptionsParser implements TDAParsing {
	private final ErrorReporter errors;
	private final AjaxSubscribe sub;

	public AjaxSubscribeOptionsParser(ErrorReporter errors, AjaxSubscribe sub) {
		this.errors = errors;
		this.sub = sub;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(errors, toks);
		if (kw == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser();
		}
		switch (kw.text) {
		case "responses": {
			if (toks.hasMoreContent()) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			return new AjaxSubscribeResponsesParser(errors, sub);
		}
		case "html": {
			if (toks.hasMoreContent()) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser();
			}
			return new AjaxSubscribeHtmlResponsesParser(errors, sub);
		}
		default: {
			errors.message(kw.location, "unrecognized ajax action " + kw.text);
			return new IgnoreNestedParser();
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
