package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.form.JSExpr;

public class JSStyleIf {
	public final JSExpr cond;
	public final JSExpr style;

	public JSStyleIf(JSExpr cond, JSExpr style) {
		this.cond = cond;
		this.style = style;
	}

}
