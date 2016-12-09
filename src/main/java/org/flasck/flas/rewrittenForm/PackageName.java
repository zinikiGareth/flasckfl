package org.flasck.flas.rewrittenForm;

public class PackageName implements NameOfThing, Comparable<PackageName> {
	private final String name;

	public PackageName(String s) {
		this.name = s;
	}
	
	@Override
	public String jsName() {
		return name;
	}

	public String simpleName() {
		return name;
	}

	@Override
	public int compareTo(PackageName o) {
		return name.compareTo(o.name);
	}

}
