package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSResponseWithMessages implements JSExpr {
	private final JSExpr call;

	public JSResponseWithMessages(JSExpr call) {
		this.call = call;
	}

	@Override
	public String asVar() {
		return "ResponseWithMessages.response(_cxt, " + call.asVar() + ")";
	}

	@Override
	public void write(IndentWriter w) {
		throw new NotImplementedException();
	}

}
