package org.flasck.flas.commonBase;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.types.FunctionType;
import org.flasck.flas.types.Type;

public abstract class TypeWithMethods extends Type {
	public TypeWithMethods(InputPosition kw, InputPosition location, WhatAmI iam, NameOfThing type, List<Type> polys) {
		super(kw, location, iam, type, polys);
	}

	public abstract boolean hasMethod(String named);

	public abstract FunctionType getMethodType(String named);
}
