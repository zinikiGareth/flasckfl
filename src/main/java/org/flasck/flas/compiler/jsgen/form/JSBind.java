package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSBind implements JSExpr {
	private final String slot;
	private final String var;

	public JSBind(String slot, String var) {
		this.slot = slot;
		this.var = var;
	}

	@Override
	public String asVar() {
		return var;
	}

	@Override
	public void write(IndentWriter w) {
		w.println("const " + var + " = " + slot + ";");
	}

}
