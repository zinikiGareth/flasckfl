package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.typechecker.Type;

@SuppressWarnings("serial")
public class RWUnionTypeDefn extends Type implements Serializable {
	public final transient boolean generate;
	public final List<Type> cases = new ArrayList<Type>();

	public RWUnionTypeDefn(InputPosition location, boolean generate, String defining, List<Type> polyvars) {
		super(null, location, WhatAmI.UNION, defining, polyvars);
		this.generate = generate;
	}
	
	public RWUnionTypeDefn addCase(Type tr) {
		this.cases.add(tr);
		return this;
	}
	
	@Override
	public String toString() {
		return name();
	}
}
