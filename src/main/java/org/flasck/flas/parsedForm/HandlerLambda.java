package org.flasck.flas.parsedForm;

public class HandlerLambda implements ExternalRef {
	public final String hi;
	public final String var;

	public HandlerLambda(String hi, String var) {
		this.hi = hi;
		this.var = var;
	}

	public String uniqueName() {
		return hi + "." + var;
	}
	
	@Override
	public int compareTo(Object o) {
		return this.toString().compareTo(o.toString());
	}

	@Override
	public String toString() {
		return "HL[" + uniqueName() + "]";
	}
}
