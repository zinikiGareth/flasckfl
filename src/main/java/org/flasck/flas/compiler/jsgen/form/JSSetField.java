package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSSetField implements JSExpr {
	private final JSExpr on;
	private final String field;
	private final JSExpr value;

	public JSSetField(String field, JSExpr value) {
		this(new JSThis(), field, value);
	}

	public JSSetField(JSExpr on, String field, JSExpr value) {
		this.on = on;
		this.field = field;
		this.value = value;
	}

	@Override
	public String asVar() {
		return on.asVar() + "." + field;
	}

	@Override
	public void write(IndentWriter w) {
		w.print(on.asVar());
		w.print(".");
		w.print(field);
		w.print(" = ");
		value.write(w);
		w.println(";");
	}
}
