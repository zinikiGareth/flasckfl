package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public class InitContext implements JSExpr {

	public InitContext() {
	}

	@Override
	public void write(IndentWriter w) {
		w.println("const _cxt = runner.newContext();");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}
}
