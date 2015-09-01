package org.flasck.flas.typechecker;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.Locatable;
import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;

@SuppressWarnings("serial")
public class Type implements Serializable, Locatable {
	private final InputPosition location;
	public enum WhatAmI { REFERENCE, BUILTIN, POLYVAR, FUNCTION, TUPLE, STRUCT, UNION, INSTANCE, OBJECT, CONTRACT, CONTRACTIMPL, CONTRACTSERVICE, HANDLERIMPLEMENTS };
	public final WhatAmI iam;
	private final String name;
	private final Type type;
	private final List<Type> polys; // polymorphic arguments to REF, STRUCT, UNION, OBJECT or INSTANCE
	private final List<Type> fnargs; // arguments to function or tuple
	
	protected Type(InputPosition location, WhatAmI iam, String name, List<Type> polys) {
		this.location = location;
		this.iam = iam;
		this.name = name;
		this.type = null;
		this.polys = polys;
		this.fnargs = null;
		
		// for anything which is not an instance, all the args MUST be polymorphic vars
		if (polys != null && iam != WhatAmI.REFERENCE && iam != WhatAmI.INSTANCE)
			for (Type t : polys)
				if (t.iam != WhatAmI.POLYVAR)
					throw new UtilException("All arguments to type defn must be poly vars");
	}

	protected Type(InputPosition location, WhatAmI iam, Type type, List<Type> args) {
		this.location = location;
		this.iam = iam;
		this.name = null;
		this.type = type;
		this.polys = args;
		this.fnargs = null;
	}

	protected Type(InputPosition location, WhatAmI iam, List<Type> subtypes) {
		if (iam != WhatAmI.FUNCTION && iam != WhatAmI.TUPLE)
			throw new UtilException("Only applicable to FUNCTION and TUPLE");
		this.location = location;
		this.iam = iam;
		this.name = null;
		this.type = null;
		this.polys = null;
		this.fnargs = subtypes;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public String name() {
		if (iam == WhatAmI.INSTANCE)
			return type.name();
		else if (iam == WhatAmI.REFERENCE || iam == WhatAmI.BUILTIN || iam == WhatAmI.POLYVAR || iam == WhatAmI.STRUCT || iam == WhatAmI.UNION || iam == WhatAmI.OBJECT ||
				 iam == WhatAmI.CONTRACT || iam == WhatAmI.CONTRACTIMPL || iam == WhatAmI.CONTRACTSERVICE || iam == WhatAmI.HANDLERIMPLEMENTS)
			return name;
		else
			throw new UtilException("Cannot ask for the name of a " + iam);
	}

	public Type innerType() {
		return type;
	}

	public boolean hasPolys() {
		return polys != null && !polys.isEmpty();
	}
	
	public Collection<Type> polys() {
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
		if (iam != WhatAmI.TUPLE)
			return fnargs.size();
		else
			throw new UtilException("Can only ask for the width of a tuple");
	}
	
	public Type arg(int i) {
		if (iam != WhatAmI.FUNCTION && iam != WhatAmI.TUPLE)
			throw new UtilException("Can only ask for the argument of a function or tuple");
		return fnargs.get(i);
	}
	
	// defining a "reference" says you know a thing's name and arguments but you don't actually know anything about it
	public static Type reference(InputPosition loc, String name, List<Type> args) {
		return new Type(loc, WhatAmI.REFERENCE, name, args);
	}

	public static Type reference(InputPosition loc, String name, Type... args) {
		return new Type(loc, WhatAmI.REFERENCE, name, CollectionUtils.listOf(args));
	}
	
	// This one is DELIBERATELY not static - you need a type that you would otherwise have to pass in as "base"
	public Type instance(InputPosition loc, Type... with) {
		return new Type(loc, WhatAmI.INSTANCE, this, CollectionUtils.listOf(with));
	}

	public Type instance(InputPosition loc, List<Type> with) {
		return new Type(loc, WhatAmI.INSTANCE, this, with);
	}

	// a "builtin" is something very simple - "number" and "string" are the only obvious examples that come to mind
	// this should ONLY be called from Builtin
	public static Type builtin(InputPosition loc, String name) {
		return new Type(loc, WhatAmI.BUILTIN, name, null);
	}
	
	public static Type polyvar(InputPosition loc, String name) {
		return new Type(loc, WhatAmI.POLYVAR, name, null);
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
	
	public Object asExpr(GarneredFrom from, VariableFactory factory) {
		Map<String, TypeVar> mapping = new HashMap<String, TypeVar>();
		return convertToExpr(from, factory, mapping);
	}

	protected Object convertToExpr(GarneredFrom from, VariableFactory factory, Map<String, TypeVar> mapping) {
		switch (iam) {
		// I don't think references to types should make it this far
//		case REFERENCE: {
//			List<Object> myargs = new ArrayList<Object>();
//			for (Type t : polys)
//				myargs.add(t.convertToExpr(factory, mapping));
//			return new TypeExpr(new GarneredFrom(location), name, myargs);
//		}
		case BUILTIN:
		case CONTRACT:
		case CONTRACTIMPL:
		case CONTRACTSERVICE:
		{
			return new TypeExpr(from, this);
		}
		case STRUCT:
		case UNION:
		case INSTANCE:
		{
			List<Object> mypolys = new ArrayList<Object>();
			for (Type t : polys)
				mypolys.add(t.convertToExpr(from, factory, mapping));
			return new TypeExpr(from, this, mypolys);
		}
		case POLYVAR: {
			if (mapping.containsKey(name))
				return mapping.get(name);
			TypeVar var = factory.next();
			mapping.put(name, var);
			return var;
		}
		case FUNCTION: {
			Object ret = fnargs.get(fnargs.size()-1).convertToExpr(new GarneredFrom(this, fnargs.size()-1), factory, mapping);
			for (int i=fnargs.size()-2;i>=0;i--) {
				Object left = fnargs.get(i).convertToExpr(from, factory, mapping);
				ret = new TypeExpr(from /* TODO: as f/arg */, Type.builtin(null, "->"), left, ret);
			}
			return ret;
		}
		default:
			throw new UtilException("error: "+ iam + " " + name());
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		show(sb);
		return sb.toString();
	}

	protected void show(StringBuilder sb) {
		switch (iam) {
		case REFERENCE:
		case STRUCT:
		case UNION:
		case BUILTIN:
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
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		if (!(obj instanceof Type))
			return false;
		Type other = (Type)obj;
		if (name != null)
			return name.equals(other.name);
		else
			return type.equals(other.type);
	}
}
