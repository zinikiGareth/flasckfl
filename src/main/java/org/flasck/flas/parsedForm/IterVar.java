package org.flasck.flas.parsedForm;

public class IterVar {
	public final String definedBy;
	public final String var;

	public IterVar(String definedBy, String var) {
		this.definedBy = definedBy;
		this.var = var;
	}
	
	public String uniqueName() {
		return definedBy + "." + var;
	}
	
	@Override
	public String toString() {
		return uniqueName();
	}
}
