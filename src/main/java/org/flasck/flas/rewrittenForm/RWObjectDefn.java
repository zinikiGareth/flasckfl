package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.AsString;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.TypeWithMethods;
import org.flasck.flas.types.Type;
import org.zinutils.collections.CollectionUtils;

public class RWObjectDefn extends TypeWithMethods implements AsString, Locatable {
	public RWStateDefinition state;
	public final List<RWStructField> ctorArgs = new ArrayList<RWStructField>();
	public final List<RWObjectMethod> methods = new ArrayList<RWObjectMethod>();
	public final transient boolean generate;

	public RWObjectDefn(InputPosition location, String tn, boolean generate, Type... polys) {
		this(location, tn, generate, CollectionUtils.listOf(polys));
	}
	
	public RWObjectDefn(InputPosition location, String tn, boolean generate, List<Type> polys) {
		super(null, location, WhatAmI.OBJECT, tn, polys);
		this.generate = generate;
	}

	public void constructorArg(InputPosition pos, Type type, String name) {
		ctorArgs.add(new RWStructField(pos, false, type, name));
	}
	
	@Override
	public boolean hasMethod(String named) {
		for (RWObjectMethod m : methods)
			if (m.name.equals(named))
				return true;
		return false;
	}

	public RWObjectDefn addMethod(RWObjectMethod m) {
		methods.add(m);
		return this;
	}

	public Type getMethod(String named) {
		for (RWObjectMethod m : methods)
			if (m.name.equals(named))
				return m.type;
		return null;
	}

	@Override
	public String toString() {
		return asString();
	}

	public String asString() {
		StringBuilder sb = new StringBuilder(name());
		if (hasPolys()) {
			sb.append("[");
			String sep = "";
			for (int i=0;i<polys().size();i++) {
				sb.append(sep);
				sb.append(poly(i));
				sep = ",";
			}
			sb.append("]");
		}
		return sb.toString();
	}
}
