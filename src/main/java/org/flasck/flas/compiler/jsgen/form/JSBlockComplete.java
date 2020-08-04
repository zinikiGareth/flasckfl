package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSBlockComplete implements JSExpr {

	@Override
	public String asVar() {
		return null;
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.println("runner.checkAtEnd()");
	}

}
