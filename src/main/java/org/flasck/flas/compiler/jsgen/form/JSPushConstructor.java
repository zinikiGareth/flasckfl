package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSPushConstructor implements JSExpr {
	private final String clz;

	public JSPushConstructor(String clz) {
		this.clz = clz;
	}

	@Override
	public void write(IndentWriter w) {
		w.print(clz);
		w.print(".eval");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		// TODO Auto-generated method stub
		
	}
}
