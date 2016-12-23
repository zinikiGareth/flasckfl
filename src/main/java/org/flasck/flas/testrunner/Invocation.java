package org.flasck.flas.testrunner;

import java.util.List;

public class Invocation {
	public final String ctr;
	public final String method;
	public final List<Object> args;

	public Invocation(String ctr, String method, List<Object> args) {
		this.ctr = ctr;
		this.method = method;
		this.args = args;
	}
	
	@Override
	public String toString() {
		return "invoked " + ctr + "." + method + " " + args;
	}
}
