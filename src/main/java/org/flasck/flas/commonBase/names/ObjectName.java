package org.flasck.flas.commonBase.names;

public class ObjectName extends SolidName {

	public ObjectName(NameOfThing container, String me) {
		super(container, me);
	}

	@Override
	public NameOfThing containingCard() {
		return this;
	}
}
