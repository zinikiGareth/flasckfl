package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSMakeTuple implements JSExpr {
	private final JSExpr[] args;

	public JSMakeTuple(JSExpr... args) {
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.makeTuple(");
		boolean isFirst = true;
		for (JSExpr e : args) {
			if (isFirst)
				isFirst = false;
			else
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
