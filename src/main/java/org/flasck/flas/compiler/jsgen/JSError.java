package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSError implements JSExpr {

	private final String msg;

	public JSError(String msg) {
		this.msg = msg;
	}

	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(IndentWriter w) {
		w.println("return new FLError(_cxt, '" + msg + "');");
	}
}
