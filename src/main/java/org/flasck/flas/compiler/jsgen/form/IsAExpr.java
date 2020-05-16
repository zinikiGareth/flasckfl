package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.SolidName;
import org.zinutils.bytecode.mock.IndentWriter;

public class IsAExpr implements JSExpr {
	private final String var;
	private final String ctor;

	public IsAExpr(String var, String ctor) {
		this.var = var;
		this.ctor = ctor;
	}

	public IsAExpr(JSExpr var, SolidName ctor) {
		this.var = var.asVar();
		this.ctor = ctor.jsName();
	}

	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.isA(" + var + ", '" + ctor + "')");
	}

}
