package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.form.JSExpr;

public class JSStyleIf {
	public JSExpr cond;
	public String styles;

	public JSStyleIf(JSExpr cond, String styles) {
		this.cond = cond;
		this.styles = styles;
	}

}
