package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSLiteral implements JSExpr {
	private final String text;

	public JSLiteral(String text) {
		this.text = text;
	}

	@Override
	public void write(IndentWriter w) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String asVar() {
		return text;
	}

}