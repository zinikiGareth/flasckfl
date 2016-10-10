package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.Locatable;
import org.flasck.flas.typechecker.Type;

@SuppressWarnings("serial")
public class ObjectDefn extends TypeWithMethods implements AsString, Serializable, Locatable {
	public StateDefinition state;
	public final List<RWStructField> ctorArgs = new ArrayList<RWStructField>();
	public final List<ObjectMethod> methods = new ArrayList<ObjectMethod>();
	public final transient boolean generate;

	public ObjectDefn(InputPosition location, String tn, boolean generate, List<Type> polys) {
		super(null, location, WhatAmI.OBJECT, tn, polys);
		this.generate = generate;
	}

	public void constructorArg(InputPosition pos, Type type, String name) {
		ctorArgs.add(new RWStructField(pos, false, type, name));
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
