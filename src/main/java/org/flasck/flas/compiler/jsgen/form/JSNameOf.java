package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSNameOf implements JSExpr {
	private final JSExpr expr;

	public JSNameOf(JSExpr expr) {
		this.expr = expr;
	}

	@Override
	public String asVar() {
		return "'" + expr.asVar() + "'";
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		expr.write(w, null);
	}
}
