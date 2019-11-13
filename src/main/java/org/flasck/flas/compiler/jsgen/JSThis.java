package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSThis implements JSExpr {
	private final String field;
	private final JSExpr value;

	public JSThis(String field, JSExpr value) {
		this.field = field;
		this.value = value;
	}

	@Override
	public String asVar() {
		return "this." + field;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("this.");
		w.print(field);
		w.print(" = ");
		value.write(w);
		w.println(";");
	}
}
