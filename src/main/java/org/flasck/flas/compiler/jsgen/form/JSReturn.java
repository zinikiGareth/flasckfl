package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSReturn implements JSExpr {
	private final JSExpr jsExpr;

	public JSReturn(JSExpr jsExpr) {
		this.jsExpr = jsExpr;
	}

	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.print("return ");
		w.print(jsExpr.asVar());
		w.println(";");
		if (jvm != null)
			jvm.returnExpr(jsExpr);
	}
}
