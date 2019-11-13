package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSCallMethod implements JSExpr {

	private final JSExpr obj;
	private final String toCall;
	private final JSExpr[] args;

	public JSCallMethod(JSExpr obj, String toCall, JSExpr... args) {
		this.obj = obj;
		this.toCall = toCall;
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
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
		w.print(")");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}
}
