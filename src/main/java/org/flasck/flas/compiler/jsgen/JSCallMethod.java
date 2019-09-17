package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSCallMethod implements JSExpr {

	private final JSExpr obj;
	private final String toCall;
	private final JSExpr[] args;
	private final JSMethod meth;
	private String var;

	public JSCallMethod(JSMethod meth, JSExpr obj, String toCall, JSExpr... args) {
		this.meth = meth;
		this.obj = obj;
		this.toCall = toCall;
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
		if (var == null)
			var = meth.obtainNextVar();
		w.print("const " + var + " = ");
		if (obj != null) {
			obj.write(w);
			w.print(".");
		}
		w.print(toCall);
		w.print("(_cxt");
		for (JSExpr e : args) {
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
