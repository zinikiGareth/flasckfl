package org.flasck.flas.newtypechecker;

public class NamedType extends TypeInfo {
	public final String name;

	public NamedType(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}
}
