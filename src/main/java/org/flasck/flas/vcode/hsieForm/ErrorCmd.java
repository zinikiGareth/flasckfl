package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;

public class ErrorCmd extends HSIEBlock {
	
	public ErrorCmd(InputPosition loc) {
		super(loc);
	}

	@Override
	public String toString() {
		return "ERROR";
	}
}
