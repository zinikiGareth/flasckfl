package org.flasck.flas.parsedForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class AbsoluteVar implements Serializable, ExternalRef {
	public final InputPosition location;
	public final String id;
	public final Object defn;

	@Deprecated
	public AbsoluteVar(InputPosition location, String id) {
		this.location = location;
		this.id = id;
		this.defn = null; // not a good idea
	}

	public AbsoluteVar(InputPosition location, String id, Object defn) {
		this.location = location;
		this.id = id;
		this.defn = defn;
	}

	public AbsoluteVar(InputPosition location, ScopeEntry entry) {
		if (location == null)
			System.out.println("null location");
		this.location = location != null ? location : entry.location();
		this.id = entry.getKey();
		this.defn = entry.getValue();
	}
	
	public AbsoluteVar(ScopeEntry entry) {
		this.location = entry.location();
		this.id = entry.getKey();
		this.defn = entry.getValue();
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
		return this.toString().compareTo(o.toString());
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof AbsoluteVar && this.toString().equals(obj.toString());
	}

	public boolean fromHandler() {
		throw new UtilException("This is not available");
	}

	@Override
	public String toString() {
		return id;
	}
}
