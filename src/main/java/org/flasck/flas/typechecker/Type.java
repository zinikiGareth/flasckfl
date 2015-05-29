package org.flasck.flas.typechecker;

import java.util.List;

import org.zinutils.exceptions.UtilException;

public class Type {
	private enum WhatAmI { SIMPLE, POLYVAR, FUNCTION, TUPLE };
	private final WhatAmI iam;
	private final String name;
	private final List<Type> args;
	
	private Type(WhatAmI iam, String name, List<Type> args) {
		this.iam = iam;
		this.name = name;
		this.args = args;
	}

	public static Type simple(String name, List<Type> args) {
		return new Type(WhatAmI.SIMPLE, name, args);
	}
	
	public static Type polyvar(String name) {
		return new Type(WhatAmI.POLYVAR, name, null);
	}
	
	public static Type function(List<Type> args) {
		return new Type(WhatAmI.FUNCTION, null, args);
	}
	
	public static Type tuple(List<Type> args) {
		return new Type(WhatAmI.TUPLE, null, args);
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
