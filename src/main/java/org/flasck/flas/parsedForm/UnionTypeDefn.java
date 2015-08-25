package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.typechecker.Type;
import org.zinutils.collections.CollectionUtils;

@SuppressWarnings("serial")
public class UnionTypeDefn extends Type implements Serializable {
	public final transient boolean generate;
	public final List<Type> cases = new ArrayList<Type>();

	public UnionTypeDefn(InputPosition location, boolean generate, String defining, Type... polyvars) {
		this(location, generate, defining, CollectionUtils.listOf(polyvars));
	}
	
	public UnionTypeDefn(InputPosition location, boolean generate, String defining, List<Type> polyvars) {
		super(location, WhatAmI.UNION, defining, polyvars);
		this.generate = generate;
	}
	
	public UnionTypeDefn addCase(Type tr) {
		this.cases.add(tr);
		return this;
	}
	
	@Override
	public String toString() {
		return name();
	}
}
