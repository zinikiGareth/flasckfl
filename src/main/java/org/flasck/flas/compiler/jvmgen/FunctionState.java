package org.flasck.flas.compiler.jvmgen;

import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public class FunctionState {
	final MethodDefiner meth;
	final IExpr fcx;
	final Var fargs;
	private int nextVar = 1;

	public FunctionState(MethodDefiner meth, IExpr fcx, Var fargs) {
		this.meth = meth;
		this.fcx = fcx;
		this.fargs = fargs;
	}

	public String nextVar(String pfx) {
		return pfx + nextVar++;
	}
}
