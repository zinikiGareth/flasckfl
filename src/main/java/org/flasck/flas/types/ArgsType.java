package org.flasck.flas.types;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class ArgsType extends Type {

	// TODO: I think "args" should be down here
	public ArgsType(InputPosition loc, WhatAmI wai, List<Type> args) {
		super(loc, wai, args);
	}

}
