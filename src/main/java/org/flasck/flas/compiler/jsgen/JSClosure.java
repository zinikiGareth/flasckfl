package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSClosure implements JSExpr {
	private final JSMethod meth;
	private final JSExpr[] args;
	private String var;

	public JSClosure(JSMethod meth, JSExpr... args) {
		this.meth = meth;
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
		if (var == null)
			var = meth.obtainNextVar();
		w.print("const " + var + " = ");
		w.print("FLEval.closure(");
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
