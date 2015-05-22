package org.flasck.flas;

import org.flasck.flas.blockForm.InputPosition;

public class FLASError {
	public final InputPosition loc;
	public final String msg;

	public FLASError(InputPosition loc, String msg) {
		this.loc = loc;
		this.msg = msg;
	}
	
	@Override
	public String toString() {
		return "" + loc + ": " + msg;
	}
}
