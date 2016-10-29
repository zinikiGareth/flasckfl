package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;

public class Head extends HSIEBlock {
	public final Var v;

	public Head(InputPosition loc, Var v) {
		super(loc);
		this.v = v;
	}

	@Override
	public String toString() {
		return "HEAD " + v;
	}
}