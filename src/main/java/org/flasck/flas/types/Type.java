package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.NameOfThing;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.NotImplementedException;
import org.zinutils.exceptions.UtilException;

public abstract class Type implements Locatable {
	public final InputPosition kw;
	private final InputPosition location;

	protected Type(InputPosition location) {
		this.kw = null;
		if (location == null)
			throw new UtilException("Type without input location 3");
		this.location = location;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public String name() { throw new NotImplementedException(); }
	public NameOfThing getTypeName() { throw new NotImplementedException(); }

	public boolean hasPolys() { return false; }
	public List<Type> polys() { throw new NotImplementedException(); }
	public Type poly(int i) { throw new NotImplementedException(); }

	// This one is DELIBERATELY not static - you need a type that you would otherwise have to pass in as "base"
	public Type instance(InputPosition loc, Type... with) {
		return new InstanceType(loc, this, CollectionUtils.listOf(with));
	}

	public Type instance(InputPosition loc, List<Type> with) {
		return new InstanceType(loc, this, with);
	}

	// a "primitive" is something very simple - "number" and "string" are the only obvious examples that come to mind
	// this should ONLY be called from Builtin
	@Deprecated
	public static Type primitive(InputPosition loc, NameOfThing name) {
		return new PrimitiveType(loc, name);
	}
	
	@Deprecated
	public static Type polyvar(InputPosition loc, String name) {
		if (loc == null)
			throw new UtilException("Type without input location 4");
		return new PolyVar(loc, name);
	}
	
	@Deprecated
	public static FunctionType function(InputPosition loc, List<Type> args) {
		return new FunctionType(loc, args);
	}

	@Deprecated
	public static FunctionType function(InputPosition loc, Type... args) {
		return new FunctionType(loc, CollectionUtils.listOf(args));
	}
	
	@Deprecated
	public static TupleType tuple(InputPosition loc, List<Type> args) {
		return new TupleType(loc, args);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		show(sb);
		return sb.toString();
	}

	protected abstract void show(StringBuilder sb);

//	public Type applyInstanceVarsFrom(Type existing) {
//		return instance(location, existing.polys);
//	}
//	
	// This should be overriden a lot more
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof Type))
			return false;
		return true;
	}
}
