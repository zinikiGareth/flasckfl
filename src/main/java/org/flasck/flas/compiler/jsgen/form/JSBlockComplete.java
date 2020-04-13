package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSBlockComplete implements JSExpr {

	@Override
	public String asVar() {
		return null;
	}

	@Override
	public void write(IndentWriter w) {
		w.println("runner.checkAtEnd()");
	}

}
