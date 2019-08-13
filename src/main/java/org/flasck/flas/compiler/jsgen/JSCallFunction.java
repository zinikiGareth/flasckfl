package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSCallFunction implements JSExpr {
	private final String fn;
	private final JSExpr[] args;

	public JSCallFunction(String fn, JSExpr... args) {
		this.fn = fn;
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("const v1 = ");
		w.print(fn);
		w.print("(");
		boolean isFirst = true;
		for (JSExpr e : args) {
			if (isFirst)
				isFirst = false;
			else
				w.print(", ");
			w.print(e.asVar());
		}
		w.println(");");
	}

	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}
}
