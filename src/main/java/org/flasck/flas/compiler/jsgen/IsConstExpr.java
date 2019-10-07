package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class IsConstExpr implements JSExpr {
	private final String var;
	private final int cnst;

	public IsConstExpr(String var, int cnst) {
		this.var = var;
		this.cnst = cnst;
	}

	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("(" + var + " == " + cnst + ")");
	}

}
