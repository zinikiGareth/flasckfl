package org.flasck.flas.types;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.PolyName;

public class PolyVar extends TypeWithName {

	public PolyVar(InputPosition loc, String name) {
		super(loc, loc, new PolyName(name));
	}

}
