package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;
import org.zinutils.utils.Justification;

public class Head extends HSIEBlock {
	public final Var v;

	public Head(InputPosition loc, Var v) {
		super(loc);
		this.v = v;
	}

	@Override
	public String toString() {
		return Justification.LEFT.format("HEAD " + v, 60) + " ?? - should be location of defn in first equation to have this as a free var";
	}
}