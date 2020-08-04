package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSHead implements JSExpr {
	private final String var;

	public JSHead(String var) {
		this.var = var;
	}

	@Override
	public String asVar() {
		return var;
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.println(var + " = _cxt.head(" + var + ");");
	}

}
