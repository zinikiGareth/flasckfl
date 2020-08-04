package org.flasck.flas.compiler.jsgen.form;

import java.util.List;

import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSXCurry implements JSExpr {
	private final List<XCArg> args;
	private final int required;

	public JSXCurry(int required, List<XCArg> posargs) {
		this.required = required;
		this.args = posargs;
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.print("_cxt.xcurry(" + required);
		for (XCArg e : args) {
			w.print(", ");
			w.print(Integer.toString(e.arg));
			w.print(", ");
			w.print(e.expr.asVar());
		}
		w.print(")");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}
}
