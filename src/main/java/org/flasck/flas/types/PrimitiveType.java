package org.flasck.flas.types;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.SolidName;

public class PrimitiveType extends TypeWithName {

	public PrimitiveType(InputPosition loc, SolidName name) {
		super(loc, loc, name);
	}

}
