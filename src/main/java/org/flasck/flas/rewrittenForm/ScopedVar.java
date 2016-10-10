package org.flasck.flas.rewrittenForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class ScopedVar implements Serializable, ExternalRef {
	public final InputPosition location;
	public final String id;
	public final Object defn;
	public boolean definedLocally;

	public ScopedVar(InputPosition location, String id, Object defn, boolean definedLocally) {
		if (defn != null && location == null)
			System.out.println("null location sv1");
		this.location = location;
		this.id = id;
		this.defn = defn;
		this.definedLocally = definedLocally;
	}

	public ScopedVar(ScopeEntry entry, boolean definedLocally) {
		if (entry.location() == null)
			System.out.println("null location sv2");
		this.location = entry.location();
		this.id = entry.getKey();
		this.defn = entry.getValue();
		this.definedLocally = definedLocally;
	}

	public ScopedVar notLocal() {
		return new ScopedVar(location, id, defn, false);
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
