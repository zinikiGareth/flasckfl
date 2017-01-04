package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NameOfThing;

public class TypeWithNameAndPolys extends TypeWithName {

	public TypeWithNameAndPolys(InputPosition kw, InputPosition location, WhatAmI iam, NameOfThing type, List<Type> polys) {
		super(kw, location, iam, type, polys);
	}
	protected void show(StringBuilder sb) {
		sb.append(name);
		showPolys(sb);
	}
}
