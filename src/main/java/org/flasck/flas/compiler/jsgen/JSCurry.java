package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSCurry implements JSExpr {
	private final int required;
	private final JSExpr[] args;

	public JSCurry(int required, JSExpr... args) {
		this.required = required;
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.curry(" + required);
		for (JSExpr e : args) {
			w.print(", ");
			w.print(e.asVar());
		}
		w.print(")");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}
}
