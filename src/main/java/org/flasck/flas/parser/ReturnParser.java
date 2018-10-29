package org.flasck.flas.parser;

import org.flasck.flas.errors.ErrorReporter;

public class ReturnParser {
	private TDAParsing ret;

	public ReturnParser() {
		this.ret = null;
	}
	
	public void with(TDAParsing ov) {
		ret = ov;
	}

	public void noNest(ErrorReporter errors) {
		ret = new NoNestingParser(errors);
	}

	public TDAParsing get() {
		return ret;
	}

}
