package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class LocalVar {
	public final InputPosition location;
	public final String definedBy;
	public final String var;

	public LocalVar(InputPosition location, String definedBy, String var) {
		this.location = location;
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
