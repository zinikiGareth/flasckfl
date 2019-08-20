package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSConstant implements JSExpr {
	private String name;

	public JSConstant(JSMethod jsMethod, String name) {
		this.name = name;
	}

	@Override
	public String asVar() {
		return name;
	}

	@Override
	public void write(IndentWriter w) {
	}
}
