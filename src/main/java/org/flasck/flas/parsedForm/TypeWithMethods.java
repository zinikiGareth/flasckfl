package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.typechecker.Type;

@SuppressWarnings("serial")
public abstract class TypeWithMethods extends Type {

	public TypeWithMethods(InputPosition location, WhatAmI iam, String type, Object object) {
		super(location, iam, type, null);
	}

	public abstract boolean hasMethod(String named);
}
