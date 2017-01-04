package org.flasck.flas.types;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NameOfThing;

public class PrimitiveType extends Type {

	public PrimitiveType(InputPosition loc, NameOfThing name) {
		super(loc, loc, WhatAmI.PRIMITIVE, name, null);
	}

}
