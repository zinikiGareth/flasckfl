package org.flasck.flas.newtypechecker;

import org.flasck.flas.blockForm.InputPosition;

public class TypeIndirect extends TypeInfo {
	public final String other;
	public final InputPosition location;

	public TypeIndirect(InputPosition location, String other) {
		this.location = location;
		this.other = other;
	}

	@Override
	public String toString() {
		return "Indirect[" + other + "]";
	}
}
