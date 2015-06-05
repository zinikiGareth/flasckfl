package org.flasck.flas.typechecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zinutils.collections.CollectionUtils;
import org.zinutils.exceptions.UtilException;

public class Type {
	public enum WhatAmI { SIMPLE, POLYVAR, FUNCTION, TUPLE };
	public final WhatAmI iam;
	private final String name;
	private final List<Type> args;
	
	private Type(WhatAmI iam, String name, List<Type> args) {
		this.iam = iam;
		this.name = name;
		this.args = args;
	}

	public String name() {
		if (iam == WhatAmI.SIMPLE || iam == WhatAmI.POLYVAR)
			return name;
		else
			throw new UtilException("Can only ask for the name of a simple of polymorphic type");
	}
	
	public int arity() {
		if (iam == WhatAmI.FUNCTION)
			return args.size() - 1;
		else
			throw new UtilException("Can only ask for the arity of a function");
	}
	
	public int width() {
		if (iam != WhatAmI.TUPLE)
			return args.size();
		else
			throw new UtilException("Can only ask for the arity of a function");
	}
	
	public Type arg(int i) {
		if (iam != WhatAmI.FUNCTION && iam != WhatAmI.TUPLE)
			throw new UtilException("Can only ask for the argument of a function or tuple");
		return args.get(i);
	}
	
	public static Type simple(String name, List<Type> args) {
		return new Type(WhatAmI.SIMPLE, name, args);
	}

	public static Type simple(String name, Type... args) {
		return new Type(WhatAmI.SIMPLE, name, CollectionUtils.listOf(args));
	}
	
	public static Type polyvar(String name) {
		return new Type(WhatAmI.POLYVAR, name, null);
	}
	
	public static Type function(List<Type> args) {
		return new Type(WhatAmI.FUNCTION, null, args);
	}

	public static Type function(Type... args) {
		return Type.function(CollectionUtils.listOf(args));
	}
	
	public static Type tuple(List<Type> args) {
		return new Type(WhatAmI.TUPLE, null, args);
	}
	
	public Object asExpr(VariableFactory factory) {
		Map<String, TypeVar> mapping = new HashMap<String, TypeVar>();
		return convertToExpr(factory, mapping);
	}

	protected Object convertToExpr(VariableFactory factory, Map<String, TypeVar> mapping) {
		switch (iam) {
		case SIMPLE: {
			List<Object> myargs = new ArrayList<Object>();
			for (Type t : args)
				myargs.add(t.convertToExpr(factory, mapping));
			return new TypeExpr(name, myargs);
		}
		case POLYVAR: {
			if (mapping.containsKey(name))
				return mapping.get(name);
			TypeVar var = factory.next();
			mapping.put(name, var);
			return var;
		}
		case FUNCTION: {
			Object ret = args.get(args.size()-1).convertToExpr(factory, mapping);
			for (int i=args.size()-2;i>=0;i--) {
				Object left = args.get(i).convertToExpr(factory, mapping);
				ret = new TypeExpr("->", left, ret);
			}
			return ret;
		}
		default:
			throw new UtilException("error: "+ iam);
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		show(sb);
		return sb.toString();
	}

	protected void show(StringBuilder sb) {
		if (iam == WhatAmI.SIMPLE) {
			sb.append(name);
			if (args != null && !args.isEmpty()) {
				sb.append("[");
				showArgs(sb, ",");
				sb.append("]");
			}
		} else if (iam == WhatAmI.POLYVAR) {
			sb.append(name);
		} else if (iam == WhatAmI.FUNCTION) {
			showArgs(sb, "->");
		} else if (iam == WhatAmI.TUPLE) {
			sb.append("(");
			showArgs(sb, ",");
			sb.append(")");
		} else
			throw new UtilException("Cannot handle " + iam);
	}

	private void showArgs(StringBuilder sb, String withSep) {
		String sep = "";
		for (Type t : args) {
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
}
