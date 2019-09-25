package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSConstant implements JSExpr {
	private final JSMethod meth;
	private final String name;
	private String var;

	public JSConstant(JSMethod jsMethod, String name) {
		this.meth = jsMethod;
		this.name = name;
	}

	@Override
	public String asVar() {
		if (var == null)
			var = meth.obtainNextVar();
		return var;
	}

	@Override
	public void write(IndentWriter w) {
		if (var == null)
			var = meth.obtainNextVar();
		w.println("const " + var + " = " + name + ".eval(_cxt);");
	}
}
