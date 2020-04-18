package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSLoadField implements JSExpr {
	private final JSExpr container;
	private final String field;

	public JSLoadField(JSExpr container, String field) {
		if (container == null) {
			throw new NotImplementedException();
		}
		this.container = container;
		this.field = field;
	}

	@Override
	public String asVar() {
		return container.asVar() + ".state.get('" + field + "')";
	}

	@Override
	public void write(IndentWriter w) {
		throw new RuntimeException("You shouldn't write this");
	}
}
