package org.flasck.flas.vcode.hsieForm;


public class BindCmd extends HSIEBlock {
	public final Var bind;
	public final Var from;
	public final String field;

	public BindCmd(Var bind, Var from, String field) {
		this.bind = bind;
		this.from = from;
		this.field = field;
	}

	@Override
	public String toString() {
		return "BIND " + bind + " " + from + "." + field;
	}
}
