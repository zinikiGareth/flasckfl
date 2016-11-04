package org.flasck.flas.newtypechecker;

public class PolyInfo extends TypeInfo {
	public final String name;

	public PolyInfo(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "PV[" + name + "]";
	}
}
