package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.NameOfThing;
import org.flasck.flas.commonBase.names.PolyName;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;

public class Type implements Locatable {
	public final InputPosition kw;
	private final InputPosition location;
	public enum WhatAmI { /* REFERENCE, */PRIMITIVE, POLYVAR, FUNCTION, TUPLE, STRUCT, UNION, INSTANCE, OBJECT, CONTRACT, CONTRACTIMPL, CONTRACTSERVICE, HANDLERIMPLEMENTS, SOMETHINGELSE };
	public final WhatAmI iam;
	protected final String name;
	protected final List<Type> polys; // polymorphic arguments to REF, STRUCT, UNION, OBJECT or INSTANCE
	private NameOfThing typeName;

	protected Type(InputPosition kw, InputPosition location, WhatAmI iam, NameOfThing name, List<Type> polys) {
		this.kw = kw;
		if (location == null && iam != WhatAmI.POLYVAR)
			throw new UtilException("Type without input location 1");
		this.location = location;
		this.iam = iam;
		this.name = name.uniqueName();
		this.typeName = name;
		this.polys = polys;
		
		// for anything which is not an instance, all the args MUST be polymorphic vars
		if (polys != null && iam != WhatAmI.INSTANCE)
			for (Type t : polys)
				if (t.iam != WhatAmI.POLYVAR)
					throw new UtilException("All arguments to type defn must be poly vars");
	}

	protected Type(InputPosition location, WhatAmI iam) {
		this.kw = null;
		if (location == null)
			throw new UtilException("Type without input location 3");
		if (iam != WhatAmI.FUNCTION && iam != WhatAmI.TUPLE && iam != WhatAmI.INSTANCE)
			throw new UtilException("Only applicable to FUNCTION and TUPLE and INSTANCE");
		this.location = location;
		this.iam = iam;
		this.name = null;
		this.polys = null;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public String name() {
		if (iam == WhatAmI.PRIMITIVE || iam == WhatAmI.POLYVAR || iam == WhatAmI.STRUCT || iam == WhatAmI.UNION || iam == WhatAmI.OBJECT ||
				 iam == WhatAmI.CONTRACT || iam == WhatAmI.CONTRACTIMPL || iam == WhatAmI.CONTRACTSERVICE || iam == WhatAmI.HANDLERIMPLEMENTS)
			return name;
		else if (iam == WhatAmI.SOMETHINGELSE)
			return "typeOf(" + name + ")";
		else
			throw new UtilException("Cannot ask for the name of a " + iam);
	}
	
	public NameOfThing getTypeName() {
		if (typeName == null)
			throw new UtilException("typename is null");
		return typeName;
	}

	public boolean hasPolys() {
		return polys != null && !polys.isEmpty();
	}
	
	public List<Type> polys() {
		if (polys == null)
			throw new UtilException("Cannot obtain poly vars of " + name() + " of type " + iam);
		return polys;
	}

	public Type poly(int i) {
		if (polys == null)
			throw new UtilException("Cannot obtain poly vars of " + name() + " of type " + iam);
		return polys.get(i);
	}

	// This one is DELIBERATELY not static - you need a type that you would otherwise have to pass in as "base"
	public Type instance(InputPosition loc, Type... with) {
		return new InstanceType(loc, this, CollectionUtils.listOf(with));
	}

	public Type instance(InputPosition loc, List<Type> with) {
		return new InstanceType(loc, this, with);
	}

	// a "primitive" is something very simple - "number" and "string" are the only obvious examples that come to mind
	// this should ONLY be called from Builtin
	public static Type primitive(InputPosition loc, NameOfThing name) {
		return new Type(loc, loc, WhatAmI.PRIMITIVE, name, null);
	}
	
	public static Type polyvar(InputPosition loc, String name) {
		if (loc == null)
			throw new UtilException("Type without input location 4");
		return new Type(loc, loc, WhatAmI.POLYVAR, new PolyName(name), null);
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

	protected void show(StringBuilder sb) {
		switch (iam) {
		case STRUCT:
		case UNION:
		case OBJECT:
		case PRIMITIVE:
		case CONTRACTIMPL:
		case CONTRACT:
		case HANDLERIMPLEMENTS:
			sb.append(name);
			showPolys(sb);
			break;
		case POLYVAR:
			sb.append(name);
			break;
		case SOMETHINGELSE:
			sb.append("copyType(" + name + ")");
			break;
		default:
			throw new UtilException("Cannot handle " + iam);
		}
	}

	protected void showPolys(StringBuilder sb) {
		if (polys != null && !polys.isEmpty()) {
			sb.append(polys);
		}
	}

	public Type applyInstanceVarsFrom(Type existing) {
		return instance(location, existing.polys);
	}
	
	// This should be overriden a lot more
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof Type))
			return false;
		Type other = (Type)obj;
		// TODO: for completeness, we should check any polymorphic args
		if (name != null)
			return name.equals(other.name);
		else
			return true;
	}
}
