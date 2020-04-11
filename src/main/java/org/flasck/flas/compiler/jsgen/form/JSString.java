package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSString implements JSExpr {
	private final String text;

	public JSString(String text) {
		this.text = text;
	}

	@Override
	public void write(IndentWriter w) {
		w.print(asVar());
	}

	// TODO: handle nested quotes properly
	@Override
	public String asVar() {
		return "'" + text + "'";
	}

}
