package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.CardName;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMockCard implements JSExpr {
	private final CardName name;
	private JSLocal nameAs;

	public JSMockCard(CardName name) {
		this.name = name;
	}

	public void nameAs(JSLocal ret) {
		nameAs = ret;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.mockCard('");
		w.print(nameAs.asVar());
		w.print("', new ");
		w.print(name.jsName());
		w.print("(_cxt))");
	}
}
