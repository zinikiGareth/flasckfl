package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSStoreField implements JSExpr {

	private final String field;
	private final JSExpr expr;

	public JSStoreField(String field, JSExpr expr) {
		this.field = field;
		this.expr = expr;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("this.state.set('");
		w.print(field);
		w.print("', ");
		w.print(expr.asVar());
		w.println(");");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}
}
