package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSThis implements JSExpr {

	@Override
	public String asVar() {
		return "this";
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		throw new NotImplementedException();
	}

}
