package org.flasck.flas.types;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PolyName;

public class PolyVar extends Type {

	public PolyVar(InputPosition loc, String name) {
		super(loc, loc, WhatAmI.POLYVAR, new PolyName(name), null);
	}

}
