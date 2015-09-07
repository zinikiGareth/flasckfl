package org.flasck.flas.parsedForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class TypedPattern implements Locatable, AsString, Serializable {
	public final transient InputPosition typeLocation;
	public final String type;
	public final ExternalRef ref;
	public final transient InputPosition varLocation;
	public final String var;

	public TypedPattern(InputPosition location, String type, InputPosition vlocation, String var) {
		this.typeLocation = location;
		this.type = type;
		this.ref = null;
		this.varLocation = vlocation;
		this.var = var;
	}
	
	public TypedPattern(InputPosition location, ExternalRef type, InputPosition vlocation, String var) {
		this.typeLocation = location;
		this.type = null;
		this.ref = type;
		this.varLocation = vlocation;
		this.var = var;
	}
	
	@Override
	public InputPosition location() {
		return typeLocation;
	}
	
	@Override
	public String toString() {
		return "TypedPattern[" + (ref!=null?ref.uniqueName():type) + ":" + var +"]";
	}

	@Override
	public String asString() {
		return "(" + (ref != null ? ref.uniqueName():type) + " " + var + ")";
	}
}
