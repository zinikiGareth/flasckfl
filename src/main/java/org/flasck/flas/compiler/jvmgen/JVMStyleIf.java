package org.flasck.flas.compiler.jvmgen;

import org.zinutils.bytecode.IExpr;

public class JVMStyleIf {
	public final IExpr cond;
	public final String styles;

	public JVMStyleIf(IExpr cond, String styleString) {
		this.cond = cond;
		this.styles = styleString;
	}

}
