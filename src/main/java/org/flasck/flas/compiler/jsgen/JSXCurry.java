package org.flasck.flas.compiler.jsgen;

import java.util.List;

import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSXCurry implements JSExpr {
	private final JSMethod meth;
	private final List<XCArg> args;
	private final int required;
	private String var;

	public JSXCurry(JSMethod meth, int required, List<XCArg> posargs) {
		this.meth = meth;
		this.required = required;
		this.args = posargs;
	}

	@Override
	public void write(IndentWriter w) {
		if (var == null)
			var = meth.obtainNextVar();
		w.print("const " + var + " = ");
		w.print("_cxt.xcurry(" + required);
		for (XCArg e : args) {
			w.print(", ");
			w.print(Integer.toString(e.arg));
			w.print(", ");
			w.print(e.expr.asVar());
		}
		w.println(");");
	}

	@Override
	public String asVar() {
		if (var == null)
			var = meth.obtainNextVar();
		return var;
	}
}
