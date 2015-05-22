package org.flasck.flas;

import org.flasck.flas.blockForm.InputPosition;

public class FLASError {
	private final InputPosition loc;
	private final String msg;

	public FLASError(InputPosition loc, String msg) {
		this.loc = loc;
		this.msg = msg;
	}

}
