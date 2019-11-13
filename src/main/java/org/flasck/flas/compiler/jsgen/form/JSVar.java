package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSVar implements JSExpr {
	private final String name;

	public JSVar(String name) {
		this.name = name;
	}

	@Override
	public void write(IndentWriter w) {
		w.print(name);
	}

	@Override
	public String asVar() {
		return name;
	}
}
