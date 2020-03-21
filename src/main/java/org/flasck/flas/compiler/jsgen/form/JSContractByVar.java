package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.JSFunctionState.StateLocation;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSContractByVar implements JSExpr {
	private final String cvar;
	private StateLocation loc;

	public JSContractByVar(StateLocation loc, String cvar) {
		this.loc = loc;
		this.cvar = cvar;
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
		return from + ".store.required('" + cvar + "')";
	}

	@Override
	public void write(IndentWriter w) {
		throw new RuntimeException("You shouldn't write this");
	}
}
