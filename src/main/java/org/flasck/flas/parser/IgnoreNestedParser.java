package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.tokenizers.CommentToken;
import org.flasck.flas.tokenizers.Tokenizable;

public class IgnoreNestedParser implements TDAParsing {
	private final ErrorReporter errors;
	private final LocationTracker locTracker;

	public IgnoreNestedParser(ErrorReporter errors) {
		this(errors, null);
	}
	
	public IgnoreNestedParser(ErrorReporter errors, LocationTracker locTracker) {
		this.errors = errors;
		this.locTracker = locTracker;
	}

	@Override
	public TDAParsing tryParsing(Tokenizable toks) {
		InputPosition pos = toks.realinfo();
		errors.logParsingToken(new CommentToken(pos, toks.remainder()));
		if (locTracker != null)
			locTracker.updateLoc(pos);
		return this;
	}

	@Override
	public void scopeComplete(InputPosition location) {
	}
}
