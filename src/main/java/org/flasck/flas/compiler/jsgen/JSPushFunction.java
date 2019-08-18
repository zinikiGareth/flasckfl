package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSPushFunction implements JSExpr {
	private final JSMethod meth;
	private final String fn;
	private String var;

	public JSPushFunction(JSMethod meth, String fn) {
		this.meth = meth;
		this.fn = fn;
	}

	@Override
	public void write(IndentWriter w) {
		if (var == null)
			var = meth.obtainNextVar();
		w.print("const " + var + " = ");
		w.print(fn);
		w.println(";");
	}

	@Override
	public String asVar() {
		if (var == null)
			var = meth.obtainNextVar();
		return var;
	}
}
