package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSLoadField implements JSExpr {
	private final String field;

	public JSLoadField(String field) {
		this.field = field;
	}

	@Override
	public String asVar() {
		return "this.state.get('" + field + "')";
	}

	@Override
	public void write(IndentWriter w) {
		throw new RuntimeException("You shouldn't write this");
	}
}
