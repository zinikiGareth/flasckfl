package org.flasck.flas.vcode.hsieForm;

import org.flasck.flas.blockForm.InputPosition;

public class BindCmd extends HSIEBlock {
	public final Var bind;
	public final Var from;
	public final String field;

	public BindCmd(InputPosition loc, Var bind, Var from, String field) {
		super(loc);
		this.bind = bind;
		this.from = from;
		this.field = field;
	}

	@Override
	public String toString() {
		return "BIND " + bind + " " + from + "." + field;
	}
}
