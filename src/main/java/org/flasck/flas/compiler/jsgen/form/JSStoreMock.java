package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSStoreMock implements JSExpr {
	private final JSExpr value;

	public JSStoreMock(JSExpr value) {
		this.value = value;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("Store in a local");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.storeMock(");
		w.print(value.asVar());
		w.print(");");
	}

}
