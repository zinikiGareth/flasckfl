package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSContractByVar implements JSExpr {
	private final JSExpr container;
	private final String cvar;

	public JSContractByVar(JSExpr container, String cvar) {
		this.container = container;
		this.cvar = cvar;
	}

	@Override
	public String asVar() {
		return container.asVar() + "._contracts.required(_cxt, '" + cvar + "')";
	}

	@Override
	public void write(IndentWriter w) {
		throw new RuntimeException("You shouldn't write this");
	}
}
