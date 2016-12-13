package org.flasck.flas.commonBase.names;

import org.flasck.flas.commonBase.NameOfThing;

public class StructName implements NameOfThing {
	private final NameOfThing container;
	private final String name;

	public StructName(NameOfThing container, String name) {
		this.container = container;
		this.name = name;
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

}
