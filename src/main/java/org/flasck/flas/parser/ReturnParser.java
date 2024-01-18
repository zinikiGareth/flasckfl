package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorReporter;

public class ReturnParser {
	private final ErrorReporter errors;
	private TDAParsing ret;

	public ReturnParser(ErrorReporter errors) {
		this.errors = errors;
		this.ret = null;
	}
	
	public void with(TDAParsing ov) {
		ret = ov;
	}

	public void noNest(ErrorReporter errors) {
		ret = new NoNestingParser(errors);
	}

	public void ignore() {
		ret = new IgnoreNestedParser(errors);
	}

	public TDAParsing get() {
		return ret;
	}

}
