package org.flasck.flas.parsedForm;

public class UnresolvedVar {
	public final String var;

	public UnresolvedVar(String var) {
		this.var = var;
	}

	@Override
	public String toString() {
		return var;
	}
}
