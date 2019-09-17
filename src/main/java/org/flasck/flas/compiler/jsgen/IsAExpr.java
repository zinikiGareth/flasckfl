package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class IsAExpr implements JSExpr {
	private final String var;
	private final String ctor;

	public IsAExpr(String var, String ctor) {
		this.var = var;
		this.ctor = ctor;
	}

	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("FLEval.isA(" + var + ", '" + ctor + "')");
	}

}
