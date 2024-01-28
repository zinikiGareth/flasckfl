package org.flasck.flas.parser.st;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.blocker.TDAParsingWithAction;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.st.AjaxSubscribe;
import org.flasck.flas.parser.IgnoreNestedParser;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.BlockLocationTracker;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.KeywordToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class AjaxSubscribeOptionsParser extends BlockLocationTracker implements TDAParsing {
	private final AjaxSubscribe sub;

	public AjaxSubscribeOptionsParser(ErrorReporter errors, AjaxSubscribe sub, LocationTracker parentTracker) {
		super(errors, parentTracker);
		this.sub = sub;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		KeywordToken kw = KeywordToken.from(errors, toks);
		if (kw == null) {
			errors.message(toks, "syntax error");
			return new IgnoreNestedParser(errors);
		}
		switch (kw.text) {
		case "responses": {
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser(errors);
			}
			return new TDAParsingWithAction(
				new AjaxSubscribeResponsesParser(errors, sub, this),
				reduction(kw.location, "ajax-responses-block")
			);
		}
		case "html": {
			if (toks.hasMoreContent(errors)) {
				errors.message(toks, "syntax error");
				return new IgnoreNestedParser(errors);
			}
			return new TDAParsingWithAction(
				new AjaxSubscribeHtmlResponsesParser(errors, sub, this),
				reduction(kw.location, "ajax-html-block")
			);
					
		}
		default: {
			errors.message(kw.location, "unrecognized ajax action " + kw.text);
			return new IgnoreNestedParser(errors);
		}
		}
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
