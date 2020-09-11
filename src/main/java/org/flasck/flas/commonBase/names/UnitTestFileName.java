package org.flasck.flas.commonBase.names;

public class UnitTestFileName implements NameOfThing {
	private final PackageName container;
	private final String name;

	public UnitTestFileName(PackageName container, String s) {
		this.container = container;
		this.name = s;
	}
	
	@Override
	public NameOfThing container() {
		return container;
	}
	
	@Override
	public PackageName packageName() {
		return container;
	}

	@Override
	public NameOfThing containingCard() {
		return null;
	}
	
	public String uniqueName() {
		return container.uniqueName() + "." + name;
	}
	
	@Override
	public String jsName() {
		return container.uniqueName() + "." + name;
	}

	@Override
	public String javaName() {
		return container.uniqueName() + "." + name;
	}

	@Override
	public String javaPackageName() {
		return container.uniqueName();
	}

	@Override
	public String javaClassName() {
		return javaName();
	}

	public String baseName() {
		return name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof UnitTestFileName))
			return false;
		UnitTestFileName other = (UnitTestFileName) obj;
		if (!other.container.equals(container))
			return false;
		return name.equals(other.name);
	}
	
	@Override
	public int hashCode() {
		return container.hashCode() ^ name.hashCode();
	}

	@Override
	public <T extends NameOfThing> int compareTo(T obj) {
		UnitTestFileName other = (UnitTestFileName) obj;
		int v1 = container.compareTo(other.container);
		if (v1 != 0)
			return v1;
		return name.compareTo(other.name);
	}
}
