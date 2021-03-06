package org.flasck.flas.commonBase.names;

import org.zinutils.exceptions.NotImplementedException;

public class PolyName implements NameOfThing, Comparable<PolyName> {
	private final String name;

	public PolyName(String s) {
		this.name = s;
	}
	
	@Override
	public String baseName() {
		return name;
	}

	@Override
	public NameOfThing containingCard() {
		return null;
	}
	
	@Override
	public PackageName packageName() {
		throw new NotImplementedException();
	}

	public String uniqueName() {
		return name;
	}
	
	@Override
	public String jsName() {
		return name;
	}

	public String simpleName() {
		return name;
	}
	
	@Override
	public String javaName() {
		throw new NotImplementedException();
	}

	@Override
	public String javaClassName() {
		throw new NotImplementedException();
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof PolyName && name.equals(((PolyName)obj).name);
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public int compareTo(PolyName o) {
		return name.compareTo(o.name);
	}
	
	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof PolyName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((PolyName)other);
	}

	@Override
	public String javaPackageName() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}

	@Override
	public NameOfThing container() {
		throw new org.zinutils.exceptions.NotImplementedException();
	}
}
