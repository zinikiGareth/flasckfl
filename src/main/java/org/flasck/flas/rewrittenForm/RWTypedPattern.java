package org.flasck.flas.rewrittenForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.typechecker.Type;

@SuppressWarnings("serial")
public class RWTypedPattern implements Locatable, AsString, Serializable {
	public final transient InputPosition typeLocation;
	public final Type type;
	public final transient InputPosition varLocation;
	public final String var;

	public RWTypedPattern(InputPosition location, Type type, InputPosition vlocation, String var) {
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
