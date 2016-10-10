package org.flasck.flas.parsedForm;

import java.io.Serializable;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class PackageVar implements Serializable, Locatable {
	public final InputPosition location;
	public final String id;
	public final Object defn;

	public PackageVar(InputPosition location, String id, Object defn) {
		if (defn != null && location == null)
			System.out.println("null location pv1");
		this.location = location;
		this.id = id;
		this.defn = defn;
	}

	public PackageVar(InputPosition location, ScopeEntry entry) {
		if (location == null)
			System.out.println("null location pv2");
		this.location = location != null ? location : entry.location();
		this.id = entry.getKey();
		this.defn = entry.getValue();
	}
	
	public PackageVar(ScopeEntry entry) {
		if (entry.location() == null)
			System.out.println("null location pv3");
		this.location = entry.location();
		this.id = entry.getKey();
		this.defn = entry.getValue();
	}
	
	public InputPosition location() {
		return location;
	}

	public String uniqueName() {
		return id;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PackageVar && this.toString().equals(obj.toString());
	}

	public boolean fromHandler() {
		throw new UtilException("This is not available");
	}

	@Override
	public String toString() {
		return id;
	}
}
