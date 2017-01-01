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
	private final Type type;
	private final List<Type> polys; // polymorphic arguments to REF, STRUCT, UNION, OBJECT or INSTANCE
	private final List<Type> fnargs; // arguments to function or tuple
	private NameOfThing typeName;

	protected Type(InputPosition kw, InputPosition location, WhatAmI iam, NameOfThing name, List<Type> polys) {
		this.kw = kw;
		if (location == null && iam != WhatAmI.POLYVAR)
			throw new UtilException("Type without input location 1");
		this.location = location;
		this.iam = iam;
		this.name = name.uniqueName();
		this.typeName = name;
		this.type = null;
		this.polys = polys;
		this.fnargs = null;
		
		// for anything which is not an instance, all the args MUST be polymorphic vars
		if (polys != null && iam != WhatAmI.INSTANCE)
			for (Type t : polys)
				if (t.iam != WhatAmI.POLYVAR)
					throw new UtilException("All arguments to type defn must be poly vars");
	}

	@Deprecated
	protected Type(InputPosition kw, InputPosition location, WhatAmI iam, String name, List<Type> polys) {
		this.kw = kw;
		if (location == null && iam != WhatAmI.POLYVAR)
			throw new UtilException("Type without input location 1");
		this.location = location;
		this.iam = iam;
		this.name = name;
		this.type = null;
		this.polys = polys;
		this.fnargs = null;
		
		// for anything which is not an instance, all the args MUST be polymorphic vars
		if (polys != null && iam != WhatAmI.INSTANCE)
			for (Type t : polys)
				if (t.iam != WhatAmI.POLYVAR)
					throw new UtilException("All arguments to type defn must be poly vars");
	}

	protected Type(InputPosition location, WhatAmI iam, Type type, List<Type> args) {
		this.kw = null;
		if (location == null && iam != WhatAmI.POLYVAR)
			throw new UtilException("Type without input location 2");
		this.location = location;
		this.iam = iam;
		this.name = null;
		this.type = type;
		this.polys = args;
		this.fnargs = null;
	}

	protected Type(InputPosition location, WhatAmI iam, List<Type> subtypes) {
		this.kw = null;
		if (location == null)
			throw new UtilException("Type without input location 3");
		if (iam != WhatAmI.FUNCTION && iam != WhatAmI.TUPLE)
			throw new UtilException("Only applicable to FUNCTION and TUPLE");
		this.location = location;
		this.iam = iam;
		this.name = null;
		this.type = null;
		this.polys = null;
		if (subtypes != null && subtypes.size() == 3 && subtypes.get(2) == null)
			System.out.println("yo");
		this.fnargs = subtypes;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public String name() {
		if (iam == WhatAmI.INSTANCE)
			return type.name();
		else if (iam == WhatAmI.PRIMITIVE || iam == WhatAmI.POLYVAR || iam == WhatAmI.STRUCT || iam == WhatAmI.UNION || iam == WhatAmI.OBJECT ||
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

	public Type innerType() {
		return type;
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

	public int arity() {
		if (iam == WhatAmI.FUNCTION)
			return fnargs.size() - 1;
		else
			throw new UtilException("Can only ask for the arity of a function");
	}
	
	public int width() {
		if (iam == WhatAmI.TUPLE)
			return fnargs.size();
		else
			throw new UtilException("Can only ask for the width of a tuple");
	}
	
	public Type arg(int i) {
		if (iam != WhatAmI.FUNCTION && iam != WhatAmI.TUPLE)
			throw new UtilException("Can only ask for the argument of a function or tuple");
		return fnargs.get(i);
	}
	
	// This one is DELIBERATELY not static - you need a type that you would otherwise have to pass in as "base"
	public Type instance(InputPosition loc, Type... with) {
		if (this.iam == WhatAmI.INSTANCE)
			throw new UtilException("Instance of an instance?  Huh?");
		return new Type(loc, WhatAmI.INSTANCE, this, CollectionUtils.listOf(with));
	}

	public Type instance(InputPosition loc, List<Type> with) {
		if (this.iam == WhatAmI.INSTANCE)
			throw new UtilException("Instance of an instance?  Huh?");
		return new Type(loc, WhatAmI.INSTANCE, this, with);
	}

	// a "builtin" is something very simple - "number" and "string" are the only obvious examples that come to mind
	// this should ONLY be called from Builtin
	public static Type primitive(InputPosition loc, NameOfThing name) {
		return new Type(loc, loc, WhatAmI.PRIMITIVE, name, null);
	}
	
	public static Type polyvar(InputPosition loc, String name) {
		if (loc == null)
			throw new UtilException("Type without input location 4");
		return new Type(loc, loc, WhatAmI.POLYVAR, new PolyName(name), null);
	}
	
	public static Type function(InputPosition loc, List<Type> args) {
		if (args.size() < 1)
			throw new UtilException("Can you have a function/method type with less than 1 arg? (the result)  Really?");
		return new Type(loc, WhatAmI.FUNCTION, args);
	}

	public static Type function(InputPosition loc, Type... args) {
		return Type.function(loc, CollectionUtils.listOf(args));
	}
	
	public static Type tuple(InputPosition loc, List<Type> args) {
		return new Type(loc, WhatAmI.TUPLE, args);
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
		case FUNCTION:
			if (fnargs.size() == 1)
				sb.append("->");
			showArgs(sb, "->");
			break;
		case TUPLE:
			sb.append("(");
			showArgs(sb, ",");
			sb.append(")");
			break;
		case INSTANCE:
			sb.append(type.name());
			showPolys(sb);
			break;
		case SOMETHINGELSE:
			sb.append("copyType(" + name + ")");
			break;
		default:
			throw new UtilException("Cannot handle " + iam);
		}
	}

	private void showPolys(StringBuilder sb) {
		if (polys != null && !polys.isEmpty()) {
			sb.append(polys);
		}
	}

	private void showArgs(StringBuilder sb, String withSep) {
		String sep = "";
		for (Type t : fnargs) {
			if (t == null) {
				sb.append("--NULL--");
				return;
			}
			sb.append(sep);
			sep = withSep;
			if (iam == WhatAmI.FUNCTION && t.iam == WhatAmI.FUNCTION) {
				sb.append("(");
				t.show(sb);
				sb.append(")");
			} else
				t.show(sb);
		}
	}

	public Type applyInstanceVarsFrom(Type existing) {
		return instance(location, existing.polys);
	}
	
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
			return type.equals(other.type);
	}
}
