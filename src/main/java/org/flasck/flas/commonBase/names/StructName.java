package org.flasck.flas.commonBase.names;

import org.flasck.flas.commonBase.NameOfThing;

public class StructName implements NameOfThing, Comparable<StructName> {
	private final NameOfThing container;
	private final String name;

	public StructName(NameOfThing container, String name) {
		this.container = container;
		this.name = name;
	}

	public String baseName() {
		return name;
	}
	
	@Override
	public String jsName() {
		if (container == null || container.jsName() == null || container.jsName().length() == 0)
			return name;
		return container.jsName() + "." + name;
	}

	@Override
	public CardName containingCard() {
		return container.containingCard();
	}

	public int compareTo(StructName other) {
		int cs = 0;
		if (container != null && other.container == null)
			return -1;
		else if (container == null && other.container != null)
			return 1;
		else if (container != null && other.container != null)
			cs = container.compareTo(other.container);
		if (cs != 0)
			return cs;
		return name.compareTo(other.name);
	}

	@Override
	public <T extends NameOfThing> int compareTo(T other) {
		if (!(other instanceof StructName))
			return other.getClass().getName().compareTo(this.getClass().getName());
		return this.compareTo((StructName)other);
	}

}
