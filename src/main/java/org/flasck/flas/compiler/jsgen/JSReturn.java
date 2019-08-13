package org.flasck.flas.compiler.jsgen;

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
	public void write(IndentWriter w) {
		w.print("return ");
		w.print(jsExpr.asVar());
		w.println(";");
	}
}
