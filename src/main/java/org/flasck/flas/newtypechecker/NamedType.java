package org.flasck.flas.newtypechecker;

import java.util.List;

public class NamedType extends TypeInfo {
	public final String name;
	public final List<TypeInfo> polyArgs;

	public NamedType(String name) {
		this.name = name;
		this.polyArgs = null; // it is not polymorphic
	}

	public NamedType(String name, List<TypeInfo> polyArgs) {
		this.name = name;
		this.polyArgs = polyArgs;
	}

	@Override
	public String toString() {
		if (polyArgs == null)
			return name;
		return name + polyArgs;
	}
}
