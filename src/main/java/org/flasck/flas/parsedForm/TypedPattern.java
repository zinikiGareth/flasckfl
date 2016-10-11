package org.flasck.flas.parsedForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;

@SuppressWarnings("serial")
public class TypedPattern implements Locatable, AsString, Serializable {
	public final transient InputPosition typeLocation;
	public final TypeReference type;
	public final transient InputPosition varLocation;
	public final String var;

	public TypedPattern(InputPosition location, TypeReference type, InputPosition vlocation, String var) {
		this.typeLocation = location;
		this.type = type;
		this.varLocation = vlocation;
		this.var = var;
	}
	
	@Override
	public InputPosition location() {
		return typeLocation;
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
