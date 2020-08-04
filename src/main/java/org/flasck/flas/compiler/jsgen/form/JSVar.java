package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSVar implements JSExpr {
	private final String name;

	public JSVar(String name) {
		this.name = name;
	}

	@Override
	public void write(IndentWriter w) {
		w.print(name);
	}

	@Override
	public String asVar() {
		return name;
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		// TODO Auto-generated method stub
		
	}
}
