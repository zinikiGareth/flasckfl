package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.typechecker.Type;
import org.zinutils.collections.CollectionUtils;

@SuppressWarnings("serial")
public class ObjectDefn extends TypeWithMethods implements AsString, Serializable, Locatable {
	public final List<ObjectMethod> methods = new ArrayList<ObjectMethod>();
	public final transient boolean generate;

	public ObjectDefn(InputPosition location, String tn, boolean generate, Type... polys) {
		this(location, tn, generate, CollectionUtils.listOf(polys));
	}
	
	public ObjectDefn(InputPosition location, String tn, boolean generate, List<Type> polys) {
		super(location, WhatAmI.OBJECT, tn, polys);
		this.generate = generate;
	}

	@Override
	public boolean hasMethod(String named) {
		for (ObjectMethod m : methods)
			if (m.name.equals(named))
				return true;
		return false;
	}

	public ObjectDefn addMethod(ObjectMethod m) {
		methods.add(m);
		return this;
	}

	public Type getMethod(String named) {
		for (ObjectMethod m : methods)
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
		if (arity() > 0) {
			sb.append("[");
			String sep = "";
			for (int i=0;i<arity();i++) {
				sb.append(sep);
				sb.append(arg(i));
				sep = ",";
			}
			sb.append("]");
		}
		return sb.toString();
	}
}
