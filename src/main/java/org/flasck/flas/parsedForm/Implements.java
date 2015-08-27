package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.typechecker.Type;

@SuppressWarnings("serial")
public class Implements extends Type {
	public final List<MethodDefinition> methods = new ArrayList<MethodDefinition>();

	public Implements(InputPosition location, WhatAmI iam, String type) {
		super(location, iam, type, null);
	}

	public void addMethod(MethodDefinition meth) {
		methods.add(meth);
	}
	
	@Override
	public String toString() {
		return name();
	}
}
