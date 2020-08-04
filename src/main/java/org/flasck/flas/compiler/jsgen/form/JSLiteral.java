package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSLiteral implements JSExpr {
	private final String text;

	public JSLiteral(String text) {
		this.text = text;
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.print(text);
	}

	@Override
	public String asVar() {
		return text;
	}

}
