package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NameOfThing;

public class TypeWithName extends Type {

	public TypeWithName(InputPosition kw, InputPosition location, WhatAmI iam, NameOfThing type, List<Type> polys) {
		super(kw, location, iam, type, polys);
	}

}
