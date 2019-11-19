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
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PosType)
			return type.equals(((PosType)obj).type);
		else
			return super.equals(obj);
	}
	
	@Override
	public String toString() {
		return pos + " ==> " + type;
	}
}
