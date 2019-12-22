package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSNewState implements JSExpr {

	public JSNewState() {
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.fields()");
	}

}
