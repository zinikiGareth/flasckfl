package org.flasck.flas.types;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NameOfThing;

public class PrimitiveType extends TypeWithName {

	public PrimitiveType(InputPosition loc, NameOfThing name) {
		super(loc, loc, name);
	}

}
