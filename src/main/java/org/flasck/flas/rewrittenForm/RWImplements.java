package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.TypeWithMethods;

public class RWImplements extends TypeWithMethods {
	public final List<RWMethodDefinition> methods = new ArrayList<RWMethodDefinition>();

	public RWImplements(InputPosition kw, InputPosition location, WhatAmI iam, String type) {
		super(kw, location, iam, type, null);
	}

	public void addMethod(RWMethodDefinition meth) {
		methods.add(meth);
	}
	
	public boolean hasMethod(String named) {
		for (RWMethodDefinition m : methods) {
			int idx = m.name().lastIndexOf('.');
			if (m.name().substring(idx+1).equals(named))
				return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return name();
	}
}
