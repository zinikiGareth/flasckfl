package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public abstract class ArgsType extends Type {
	protected final List<Type> args;

	public ArgsType(InputPosition loc, WhatAmI wai, List<Type> args) {
		super(loc);
		this.args = args;
	}

	public Type arg(int i) {
		return args.get(i);
	}
	
	protected void showArgs(StringBuilder sb, String withSep) {
		String sep = "";
		for (Type t : args) {
			if (t == null) {
				sb.append("--NULL--");
				return;
			}
			sb.append(sep);
			sep = withSep;
			if (this instanceof FunctionType && t instanceof FunctionType) {
				sb.append("(");
				t.show(sb);
				sb.append(")");
			} else
				t.show(sb);
		}
	}
}
