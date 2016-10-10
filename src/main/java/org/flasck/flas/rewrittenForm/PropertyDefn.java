package org.flasck.flas.rewrittenForm;

import org.flasck.flas.blockForm.InputPosition;

public class PropertyDefn {
	public final InputPosition location;
	public final String name;
	public final Object value;

	public PropertyDefn(InputPosition location, String name, Object value) {
		this.location = location;
		this.name = name;
		this.value = value;
	}
}
