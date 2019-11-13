package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSIfExpr implements JSExpr {
	private final JSExpr test;
	private final JSBlockCreator trueCase;
	private final JSBlockCreator falseCase;

	public JSIfExpr(JSExpr test, JSBlockCreator trueCase, JSBlockCreator falseCase) {
		this.test = test;
		this.trueCase = trueCase;
		this.falseCase = falseCase;
	}

	public JSBlockCreator trueCase() {
		return trueCase;
	}

	public JSBlockCreator falseCase() {
		return falseCase;
	}

	@Override
	public String asVar() {
		return null;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("if (");
		test.write(w);
		w.print(") ");
		trueCase.write(w);
		w.print(" else ");
		falseCase.write(w);
		w.println("");
	}
}
