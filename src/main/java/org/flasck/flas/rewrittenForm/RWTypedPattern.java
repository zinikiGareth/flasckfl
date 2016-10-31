package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.typechecker.Type;
import org.zinutils.exceptions.UtilException;

public class RWTypedPattern implements Locatable, AsString {
	public final transient InputPosition typeLocation;
	public final Type type;
	public final transient InputPosition varLocation;
	public final String var;

	public RWTypedPattern(InputPosition location, Type type, InputPosition vlocation, String var) {
		if (location == null || vlocation == null)
			throw new UtilException("Cannot have null locations");
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
