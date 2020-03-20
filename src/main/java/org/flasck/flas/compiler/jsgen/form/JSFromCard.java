package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSFromCard implements JSExpr {

	@Override
	public String asVar() {
		return "this._card";
	}

	@Override
	public void write(IndentWriter w) {
		// shouldn't happen
	}

}
