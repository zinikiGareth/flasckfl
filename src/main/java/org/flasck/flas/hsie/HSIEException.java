package org.flasck.flas.hsie;

import org.flasck.flas.blockForm.Block;

@SuppressWarnings("serial")
public class HSIEException extends RuntimeException {
	public final Block block;
	public final String msg;

	public HSIEException(Block block, String msg) {
		this.block = block;
		this.msg = msg;
	}
}
