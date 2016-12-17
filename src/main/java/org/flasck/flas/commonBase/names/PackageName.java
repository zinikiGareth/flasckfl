package org.flasck.flas.commonBase.names;

import org.flasck.flas.commonBase.NameOfThing;

public class PackageName implements NameOfThing, Comparable<PackageName> {
	private final String name;

	public PackageName(String s) {
		this.name = s;
	}
	
	@Override
	public CardName containingCard() {
		return null;
	}
	
	@Override
	public String jsName() {
		return name;
	}

	public String simpleName() {
		return name;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PackageName && name.equals(((PackageName)obj).name);
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public int compareTo(PackageName o) {
		return name.compareTo(o.name);
	}
	
	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof PackageName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((PackageName)other);
	}
}
