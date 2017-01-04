package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NameOfThing;
import org.zinutils.exceptions.UtilException;

public class TypeWithNameAndPolys extends TypeWithName {

	public TypeWithNameAndPolys(InputPosition kw, InputPosition location, WhatAmI iam, NameOfThing type, List<Type> polys) {
		super(kw, location, iam, type, polys);
	}

	public boolean hasPolys() {
		return polys != null && !polys.isEmpty();
	}
	
	public List<Type> polys() {
		if (polys == null)
			throw new UtilException("Cannot obtain poly vars of " + name() + " of type " + iam);
		return polys;
	}

	public Type poly(int i) {
		if (polys == null)
			throw new UtilException("Cannot obtain poly vars of " + name() + " of type " + iam);
		return polys.get(i);
	}

	protected void show(StringBuilder sb) {
		sb.append(name);
		showPolys(sb);
	}
}
