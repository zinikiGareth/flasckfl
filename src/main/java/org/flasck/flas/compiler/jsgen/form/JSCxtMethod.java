package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSCxtMethod implements JSExpr {

	private final String toCall;
	private final JSExpr[] args;

	public JSCxtMethod(String toCall, JSExpr... args) {
		this.toCall = toCall;
		this.args = args;
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.print("_cxt.");
		w.print(toCall);
		String sep = "(";
		for (JSExpr e : args) {
			w.print(sep);
			sep = ", ";
			w.print(e.asVar());
		}
		w.print(")");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}
}
