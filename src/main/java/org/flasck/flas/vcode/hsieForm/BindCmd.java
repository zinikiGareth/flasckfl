package org.flasck.flas.vcode.hsieForm;


public class BindCmd extends HSIEBlock {
	private final Var bind;
	private final Var from;
	private final String field;

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
