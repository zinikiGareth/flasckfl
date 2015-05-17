package org.flasck.flas.hsieForm;

import org.flasck.flas.hsieForm.HSIEForm.Var;

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