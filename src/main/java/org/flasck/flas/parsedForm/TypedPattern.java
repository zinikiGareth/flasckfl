package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;

public class TypedPattern {
	public final InputPosition typeLocation;
	public final String type;
	public final InputPosition varLocation;
	public final String var;

	public TypedPattern(InputPosition location, String type, InputPosition vlocation, String var) {
		this.typeLocation = location;
		this.type = type;
		this.varLocation = vlocation;
		this.var = var;
	}
	
	@Override
	public String toString() {
		return "TypedPattern[" + type + ":" + var +"]";
	}
}
