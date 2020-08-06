package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
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
	public void write(IndentWriter w) {
		w.print("_cxt.isTruthy(");
		w.print(expr.asVar());
		w.print(")");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		IExpr ret = jvm.method().callInterface(J.BOOLEANP.getActual(), jvm.cxt(), "isTruthy", jvm.arg(expr));
		jvm.local(this, ret);
	}

}
