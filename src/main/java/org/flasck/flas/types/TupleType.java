package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class TupleType extends ArgsType {

	public TupleType(InputPosition loc, List<Type> args) {
		super(loc, WhatAmI.TUPLE, args);
	}

	public int width() {
		return args.size();
	}

	protected void show(StringBuilder sb) {
		sb.append("(");
		showArgs(sb, ",");
		sb.append(")");
	}
}
