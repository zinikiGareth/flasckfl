package org.flasck.flas.commonBase.names;

public class PackageName implements NameOfThing, Comparable<PackageName> {
	private final String name;

	public PackageName(String s) {
		this.name = s;
	}
	
	@Override
	public NameOfThing container() {
		return null;
	}
	
	@Override
	public PackageName packageName() {
		return this;
	}

	@Override
	public String baseName() {
		return name;
	}

	@Override
	public NameOfThing containingCard() {
		return null;
	}
	
	public String uniqueName() {
		return name;
	}
	
	@Override
	public String jsName() {
		return name.replace(".", "__");
	}

	@Override
	public String javaName() {
		return name;
	}

	@Override
	public String javaPackageName() {
		return name;
	}

	@Override
	public String javaClassName() {
		return name;
	}

	public String simpleName() {
		return name;
	}

	public String finalPart() {
		int idx = name.lastIndexOf('.');
		if (idx == -1)
			return name;
		else
			return name.substring(idx+1);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof PackageName))
			return false;
		PackageName pn = (PackageName) obj;
		if (name == null && pn.name == null)
			return true;
		else if (name == null)
			return false;
		else
			return obj instanceof PackageName && name.equals(pn.name);
	}
	
	@Override
	public int hashCode() {
		if (name == null)
			return 0;
		return name.hashCode();
	}
	
	@Override
	public int compareTo(PackageName o) {
		if (name == null && o.name == null)
			return 0;
		else if (name == null)
			return -1;
		else if (o.name == null)
			return 1;
		else
			return name.compareTo(o.name);
	}
	
	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof PackageName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((PackageName)other);
	}
	
	@Override
	public String toString() {
		return "Pkg[" + name + "]";
	}
}
