package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.CardName;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMockCard implements JSExpr {
	private final CardName name;

	public JSMockCard(CardName name) {
		this.name = name;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.mockCard(new ");
		w.print(name.jsName());
		w.print("(_cxt))");
	}
}
