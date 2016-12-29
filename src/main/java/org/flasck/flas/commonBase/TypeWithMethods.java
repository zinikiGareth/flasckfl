package org.flasck.flas.commonBase;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.StructName;
import org.flasck.flas.types.Type;
import org.zinutils.exceptions.UtilException;

public abstract class TypeWithMethods extends Type {
	private StructName typeName;

	public TypeWithMethods(InputPosition kw, InputPosition location, WhatAmI iam, StructName type, List<Type> polys) {
		super(kw, location, iam, type.uniqueName(), polys);
		this.typeName = type;
	}

	public abstract boolean hasMethod(String named);

	public abstract Type getMethodType(String named);
	
	public StructName getTypeName() {
		if (typeName == null)
			throw new UtilException("typename is null");
		return typeName;
	}
}
