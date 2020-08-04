package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSMember implements JSExpr {
	private final String var;

	public JSMember(String var) {
		this.var = var;
	}

	@Override
	public String asVar() {
		return "this." + var;
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		throw new NotImplementedException();
	}

}
