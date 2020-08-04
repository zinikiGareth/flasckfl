package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class IsTrueExpr implements JSExpr {
	private JSExpr expr;

	public IsTrueExpr(JSExpr expr) {
		this.expr = expr;
	}

	@Override
	public String asVar() {
		return expr.asVar();
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.print("_cxt.isTruthy(");
		w.print(expr.asVar());
		w.print(")");
	}

}
