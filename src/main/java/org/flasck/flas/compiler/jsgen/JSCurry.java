package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSCurry implements JSExpr {
	private final JSMethod meth;
	private final JSExpr[] args;
	private final int required;
	private String var;

	public JSCurry(JSMethod meth, int required, JSExpr... args) {
		this.meth = meth;
		this.required = required;
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
		if (var == null)
			var = meth.obtainNextVar();
		w.print("const " + var + " = ");
		w.print("_cxt.curry(" + required);
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
		if (var == null)
			var = meth.obtainNextVar();
		return var;
	}
}
