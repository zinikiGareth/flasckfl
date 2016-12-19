package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.VarName;
import org.zinutils.exceptions.UtilException;

public class ScopedVar implements ExternalRef {
	public final InputPosition location;
	public final VarName myId;
	public final Object defn;
	public FunctionName definedBy;

	public ScopedVar(InputPosition location, VarName id, Object defn, FunctionName definedBy) {
		this.definedBy = definedBy;
		if (defn == null)
			throw new NullPointerException("NestedVar cannot be in a null function");
		if (location == null)
			throw new UtilException("null location sv1");
		this.location = location;
		this.myId = id; // but should be passed in
		this.defn = defn;
	}

	public InputPosition location() {
		return location;
	}

	@Override
	public String uniqueName() {
		return myId.uniqueName();
	}
	
	@Override
	public int compareTo(Object o) {
		return this.myId.uniqueName().compareTo(((ScopedVar)o).myId.uniqueName());
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ScopedVar && this.myId.equals(((ScopedVar)obj).myId);
	}

	public boolean fromHandler() {
		throw new UtilException("This is not available");
	}

	@Override
	public String toString() {
		return "Scoped[" + myId.uniqueName() + "]";
	}
}
