package org.flasck.flas.compiler.jsgen.form;

import java.util.List;

import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSExpectation implements JSExpr {
	private final JSExpr mock;
	private final String method;
	private final List<JSExpr> args;
	private final JSExpr handler;

	public JSExpectation(JSExpr mock, String method, List<JSExpr> args, JSExpr handler) {
		this.mock = mock;
		this.method = method;
		this.args = args;
		this.handler = handler;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException();
	}

	@Override
	public void write(IndentWriter w) {
		w.print(mock.asVar());
		w.print(".expect(");
		w.print("'" + method + "'");
		w.print(", [");
		String sep = "";
		for (JSExpr a : args) {
			w.print(sep);
			sep = ", ";
			w.print(a.asVar());
		}
		w.print("], ");
		w.print(handler.asVar());
		w.println(");");
	}

}
