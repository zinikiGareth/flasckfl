package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.UtilException;

public class InstanceType extends Type {
	protected final Type type;
	private List<Type> types;

	public InstanceType(InputPosition loc, Type type, List<Type> types) {
		super(loc, WhatAmI.INSTANCE);
		if (type instanceof InstanceType)
			throw new UtilException("Instance of an instance?  Huh?");
		this.type = type;
		this.types = types;
	}

	public String name() {
		return type.name();
	}

	public Type innerType() {
		return type;
	}

	public boolean hasPolys() {
		return types != null && !types.isEmpty();
	}
	
	public List<Type> polys() {
		if (types == null)
			throw new UtilException("Cannot obtain poly vars of " + name() + " of type " + iam);
		return types;
	}

	public Type poly(int i) {
		if (types == null)
			throw new UtilException("Cannot obtain poly vars of " + name() + " of type " + iam);
		return types.get(i);
	}

	protected void show(StringBuilder sb) {
		sb.append(type.name());
		if (types != null && !types.isEmpty()) {
			sb.append(types);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof InstanceType))
			return false;
		InstanceType other = (InstanceType)obj;
		// TODO: for completeness, we should check any polymorphic args
		return type.equals(other.type);
	}
}
