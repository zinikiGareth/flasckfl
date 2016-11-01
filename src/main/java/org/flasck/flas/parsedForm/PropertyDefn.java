package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class PropertyDefn implements Locatable, Comparable<PropertyDefn> {
	public final InputPosition location;
	public final String name;
	public final Object value;

	public PropertyDefn(InputPosition location, String name, Object value) {
		this.location = location;
		this.name = name;
		this.value = value;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	@Override
	public int compareTo(PropertyDefn o) {
		return name.compareTo(o.name);
	}
}
