package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSArray implements JSExpr {
	private final Iterable<JSExpr> arr;

	public JSArray(Iterable<JSExpr> arr) {
		this.arr = arr;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("need to write this");
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.print("[");
		String sep = "";
		for (JSExpr e : arr) {
			w.print(sep);
			w.print(e.asVar());
			sep = ",";
		}
		w.print("]");
	}

}
