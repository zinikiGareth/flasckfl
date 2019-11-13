package org.flasck.flas.compiler.jsgen;

import java.util.List;

import org.zinutils.bytecode.mock.IndentWriter;

// I think this and JSCreateObject are basically the same
public class JSStruct implements JSExpr {
	private final String name;
	private final List<JSExpr> args;

	public JSStruct(String name, List<JSExpr> args) {
		this.name = name;
		this.args = args;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print(name + ".eval(_cxt");
		for (JSExpr e : args) {
			w.print(", ");
			w.print(e.asVar());
		}
		w.print(")");
	}
}
