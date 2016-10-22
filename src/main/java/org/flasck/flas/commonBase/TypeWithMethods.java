package org.flasck.flas.commonBase;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.typechecker.Type;

public abstract class TypeWithMethods extends Type {

	public TypeWithMethods(InputPosition kw, InputPosition location, WhatAmI iam, String type, List<Type> polys) {
		super(kw, location, iam, type, polys);
	}

	public abstract boolean hasMethod(String named);
}
