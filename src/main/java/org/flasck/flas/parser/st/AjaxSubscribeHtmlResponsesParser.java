package org.flasck.flas.parser.st;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.st.AjaxSubscribe;
import org.flasck.flas.parser.BlockLocationTracker;
import org.flasck.flas.parser.LocationTracker;
import org.flasck.flas.parser.NoNestingParser;
import org.flasck.flas.parser.TDAParsing;
import org.flasck.flas.tokenizers.FreeTextToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class AjaxSubscribeHtmlResponsesParser extends BlockLocationTracker implements TDAParsing {
	private final AjaxSubscribe sub;

	public AjaxSubscribeHtmlResponsesParser(ErrorReporter errors, AjaxSubscribe sub, LocationTracker locTracker) {
		super(errors, locTracker);
		this.sub = sub;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		InputPosition loc = toks.realinfo();
		String text = toks.remainder();
		FreeTextToken token = new FreeTextToken(loc, text);
		errors.logParsingToken(token);
		super.tellParent(loc);
		sub.html(loc, text);
		return new NoNestingParser(errors);
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}

}
