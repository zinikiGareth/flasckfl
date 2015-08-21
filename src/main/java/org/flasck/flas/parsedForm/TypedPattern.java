package org.flasck.flas.parsedForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class TypedPattern implements AsString, Serializable {
	public final transient InputPosition typeLocation;
	public final String type;
	public final transient InputPosition varLocation;
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

	@Override
	public String asString() {
		return "(" + type + " " + var + ")";
	}
}
