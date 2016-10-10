package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class Implements extends TypeWithMethods {
	public final List<RWMethodDefinition> methods = new ArrayList<RWMethodDefinition>();

	public Implements(InputPosition kw, InputPosition location, WhatAmI iam, String type) {
		super(kw, location, iam, type, null);
	}

	public void addMethod(RWMethodDefinition meth) {
		methods.add(meth);
	}
	
	public boolean hasMethod(String named) {
		for (RWMethodDefinition m : methods) {
			int idx = m.intro.name.lastIndexOf('.');
			if (m.intro.name.substring(idx+1).equals(named))
				return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return name();
	}
}
