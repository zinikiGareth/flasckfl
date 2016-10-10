package org.flasck.flas.parsedForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.stories.FLASStory.State;
import org.flasck.flas.typechecker.Type;

@SuppressWarnings("serial")
public class ObjectDefn extends TypeWithMethods implements ContainsScope, AsString, Serializable, Locatable {
	public StateDefinition state;
	public final List<StructField> ctorArgs = new ArrayList<StructField>();
	public final List<ObjectMethod> methods = new ArrayList<ObjectMethod>();
	public final transient boolean generate;
	private final Scope innerScope;

	public ObjectDefn(InputPosition location, Scope outer, String tn, boolean generate, List<Type> polys) {
		super(null, location, WhatAmI.OBJECT, tn, polys);
		this.generate = generate;
		ScopeEntry se = outer.define(State.simpleName(tn), tn, this);
		this.innerScope = new Scope(se, this);
	}

	@Override
	public Scope innerScope() {
		return innerScope;
	}

	public void constructorArg(InputPosition pos, Type type, String name) {
		ctorArgs.add(new StructField(pos, false, type, name));
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
