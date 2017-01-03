package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.NameOfThing;
import org.flasck.flas.commonBase.TypeWithMethods;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.types.Type;
import org.zinutils.exceptions.NotImplementedException;

public class RWImplements extends TypeWithMethods {
	public final List<RWMethodDefinition> methods = new ArrayList<RWMethodDefinition>();
	public final CSName realName;

	public RWImplements(InputPosition kw, InputPosition location, WhatAmI iam, CSName realName, NameOfThing type) {
		super(kw, location, iam, type, null);
		this.realName = realName;
	}

	public void addMethod(RWMethodDefinition meth) {
		methods.add(meth);
	}
	
	public boolean hasMethod(String named) {
		for (RWMethodDefinition m : methods) {
			int idx = m.name().jsName().lastIndexOf('.');
			if (m.name().jsName().substring(idx+1).equals(named))
				return true;
		}
		return false;
	}
	
	@Override
	public Type getMethodType(String named) {
		throw new NotImplementedException("This cannot be implemented because RWMethodDefinition does not (yet?) have a type");
//		for (RWMethodDefinition md : methods)
//			if (md.name().equals(named))
//				return md.getType();
//		return null;
	}
	
	@Override
	public String toString() {
		return name();
	}
}
