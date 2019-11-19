package org.flasck.flas.tc3;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;

public class PosType implements Locatable {
	public final InputPosition pos;
	public final Type type;

	public PosType(InputPosition pos, Type type) {
		this.pos = pos;
		this.type = type;
		
	}

	@Override
	public InputPosition location() {
		return pos;
	}
}
