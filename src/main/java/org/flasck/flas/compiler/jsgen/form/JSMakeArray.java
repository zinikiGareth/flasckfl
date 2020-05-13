package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSMakeArray implements JSExpr {
	private final List<JSExpr> args;

	public JSMakeArray(JSExpr... args) {
		this.args = Arrays.asList(args);
	}

	public JSMakeArray(List<JSExpr> args) {
		this.args = new ArrayList<>(args);
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.array(");
		boolean isFirst = true;
		for (JSExpr e : args) {
			if (isFirst)
				isFirst = false;
			else
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
