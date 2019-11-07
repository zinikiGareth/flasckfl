package org.flasck.flas.compiler.jsgen;

import java.util.List;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSStruct implements JSExpr {
	private final JSMethod meth;
	private final String name;
	private final List<JSExpr> args;
	private String var;

	public JSStruct(JSMethod jsMethod, String name, List<JSExpr> args) {
		this.meth = jsMethod;
		this.name = name;
		this.args = args;
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
		w.print("const " + var + " = " + name + ".eval(_cxt");
		for (JSExpr e : args) {
			w.print(", ");
			w.print(e.asVar());
		}
		w.println(");");
	}
}
