package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.UtilException;

public class ScopedVar implements ExternalRef {
	public final InputPosition location;
	public final String id;
	public final Object defn;
	public boolean definedLocally;
	public String definedIn;

	public ScopedVar(InputPosition location, String id, Object defn, String definedBy, boolean definedLocally) {
		definedIn = definedBy;
		if (defn == null)
			throw new NullPointerException("NestedVar cannot be in a null function");
		if (location == null)
			throw new UtilException("null location sv1");
		this.location = location;
		this.id = id;
		this.defn = defn;
		this.definedLocally = definedLocally;
	}

	public ScopedVar notLocal() {
		return new ScopedVar(location, id, defn, definedIn, false);
	}

	public ScopedVar asLocal() {
		return new ScopedVar(location, id, defn, definedIn, true);
	}
	
	public InputPosition location() {
		return location;
	}

	@Override
	public String uniqueName() {
		return id;
	}
	
	@Override
	public int compareTo(Object o) {
		return this.id.compareTo(((ScopedVar)o).id);
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ScopedVar && this.id.equals(((ScopedVar)obj).id);
	}

	public boolean fromHandler() {
		throw new UtilException("This is not available");
	}

	@Override
	public String toString() {
		return "Scoped[" + id + "]";
	}
}
