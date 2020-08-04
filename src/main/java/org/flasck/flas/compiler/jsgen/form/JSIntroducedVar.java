package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSIntroducedVar implements JSExpr {
	@Override
	public String asVar() {
		return "new BoundVar()";
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.print(asVar());
	}
}