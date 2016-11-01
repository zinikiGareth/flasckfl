package org.flasck.flas.commonBase;

import org.flasck.flas.blockForm.InputPosition;

public class LocatedObject {
	public final InputPosition loc;
	public final Object obj;

	public LocatedObject(InputPosition loc, Object obj) {
		this.loc = loc;
		this.obj = obj;
	}
}