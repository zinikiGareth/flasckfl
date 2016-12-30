package org.flasck.flas.newtypechecker;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NameOfThing;

public class NamedType extends TypeInfo {
	private final InputPosition location;
	public final String name;
	public final List<TypeInfo> polyArgs;

	public NamedType(InputPosition location, NameOfThing name) {
		this.location = location;
		this.name = name.uniqueName();
		this.polyArgs = new ArrayList<TypeInfo>(); // it is not polymorphic
	}

	public NamedType(InputPosition location, NameOfThing name, List<TypeInfo> polyArgs) {
		this.location = location;
		this.name = name.uniqueName();
		this.polyArgs = polyArgs;
	}

	@Deprecated
	public NamedType(InputPosition location, String name, List<TypeInfo> polyArgs) {
		this.location = location;
		this.name = name;
		this.polyArgs = polyArgs;
	}

	public boolean equals(Object other) {
		return other instanceof NamedType && name.equals(((NamedType)other).name);
	}
	
	@Override
	public String toString() {
		if (polyArgs == null)
			return name;
		return name + (polyArgs.isEmpty()? "": polyArgs);
	}

	public InputPosition location() {
		return location;
	}
}
