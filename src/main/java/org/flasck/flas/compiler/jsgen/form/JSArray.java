package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSArray implements JSExpr {
	private final List<JSExpr> arr;

	public JSArray(List<JSExpr> arr) {
		this.arr = new ArrayList<>(arr);
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("need to write this");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("[");
		String sep = "";
		for (JSExpr e : arr) {
			w.print(sep);
			w.print(e.asVar());
			sep = ",";
		}
		w.print("]");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
	}
}
