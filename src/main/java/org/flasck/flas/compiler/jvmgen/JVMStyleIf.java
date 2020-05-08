package org.flasck.flas.compiler.jvmgen;

import org.zinutils.bytecode.IExpr;

public class JVMStyleIf {
	public final IExpr cond;
	public final IExpr style;

	public JVMStyleIf(IExpr cond, IExpr style) {
		this.cond = cond;
		this.style = style;
	}
}
