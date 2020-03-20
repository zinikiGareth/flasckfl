package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.JSFunctionState.StateLocation;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSLoadField implements JSExpr {
	private final String field;
	private StateLocation loc;

	public JSLoadField(StateLocation loc, String field) {
		this.loc = loc;
		this.field = field;
	}

	@Override
	public String asVar() {
		String from;
		switch (loc) {
		case LOCAL:
			from = "this";
			break;
		case CARD:
			from = "this._card";
			break;
		default:
			throw new NotImplementedException("cannot handle JSLoadField with " + loc);
		}
		return from + ".state.get('" + field + "')";
	}

	@Override
	public void write(IndentWriter w) {
		throw new RuntimeException("You shouldn't write this");
	}
}
