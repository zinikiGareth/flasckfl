package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.VarName;
import org.flasck.flas.types.Type;
import org.zinutils.exceptions.UtilException;

public class HandlerLambda implements ExternalRef {
	public final InputPosition location;
	public final HandlerName clzName;
	public final String var;
	public final Type type;
	public ScopedVar scopedFrom;

	public HandlerLambda(InputPosition location, HandlerName clzName, Type type, String var) {
		this.location = location;
		this.clzName = clzName;
		this.type = type;
		this.var = var;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public String uniqueName() {
		return clzName.uniqueName() + "." + var;
	}
	
	@Override
	public NameOfThing myName() {
		return new VarName(location, clzName, var);
	}

	@Override
	public int compareTo(Object o) {
		return this.toString().compareTo(o.toString());
	}
	
	public boolean fromHandler() {
		throw new UtilException("This is not available");
	}

	@Override
	public String toString() {
		return "HL[" + uniqueName() + "]";
	}
}
