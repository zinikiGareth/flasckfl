package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.exceptions.UtilException;

public class FunctionType extends Type {

	public FunctionType(InputPosition loc, List<Type> args) {
		super(loc, WhatAmI.FUNCTION, args);
		if (args.size() < 1)
			throw new UtilException("Can you have a function/method type with less than 1 arg? (the result)  Really?");
	}

	protected void show(StringBuilder sb) {
		if (fnargs.size() == 1)
			sb.append("->");
		showArgs(sb, "->");
	}
}
