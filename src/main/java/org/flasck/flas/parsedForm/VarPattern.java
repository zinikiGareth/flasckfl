package org.flasck.flas.parsedForm;

public class VarPattern {
	public final String var;

	public VarPattern(String text) {
		this.var = text;
	}
	
	@Override
	public String toString() {
		return var;
	}
}
