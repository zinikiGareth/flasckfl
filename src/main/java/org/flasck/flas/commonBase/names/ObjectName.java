package org.flasck.flas.commonBase.names;

public class ObjectName extends SolidName {

	public ObjectName(NameOfThing container, String name) {
		super(container, name);
	}
	@Override
	public NameOfThing containingCard() {
		return this;
	}
}
