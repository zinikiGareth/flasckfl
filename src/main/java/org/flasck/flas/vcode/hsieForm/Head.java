package org.flasck.flas.vcode.hsieForm;


public class Head extends HSIEBlock {
	public final Var v;

	public Head(Var v) {
		this.v = v;
	}

	@Override
	public String toString() {
		return "HEAD " + v;
	}
}