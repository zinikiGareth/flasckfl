package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;

public abstract class TypeWithMethods extends TypeWithNameAndPolys {
	public TypeWithMethods(InputPosition kw, InputPosition location, NameOfThing type, List<PolyVar> polys) {
		super(kw, location, type, polys);
	}

	public abstract boolean hasMethod(String named);

	public abstract FunctionType getMethodType(String named);
}