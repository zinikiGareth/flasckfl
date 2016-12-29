package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NameOfThing;
import org.zinutils.exceptions.UtilException;

public class PackageVar implements ExternalRef {
	public final InputPosition location;
	public final String id;
	public final Object defn;
	public final NameOfThing name;

	@Deprecated
	public PackageVar(InputPosition location, String id, Object defn) {
		if (defn != null && location == null)
			throw new UtilException("null location pv1");
		this.location = location;
		this.name = null;
		this.id = id;
		this.defn = defn;
	}

	public PackageVar(InputPosition location, NameOfThing name, Object defn) {
		if (defn != null && location == null)
			throw new UtilException("null location pv1");
		this.location = location;
		this.name = name;
		this.id = name.uniqueName();
		this.defn = defn;
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
	public int compareTo(Object o) {
		return this.id.compareTo(((ExternalRef)o).uniqueName());
	}

	@Override
	public String toString() {
		return id;
	}
}
