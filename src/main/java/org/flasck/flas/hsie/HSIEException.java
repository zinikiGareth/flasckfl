package org.flasck.flas.hsie;

import org.flasck.flas.blockForm.InputPosition;

@SuppressWarnings("serial")
public class HSIEException extends RuntimeException {
	public final InputPosition where;
	public final String msg;

	public HSIEException(InputPosition inputPosition, String msg) {
		this.where = inputPosition;
		this.msg = msg;
	}
}
