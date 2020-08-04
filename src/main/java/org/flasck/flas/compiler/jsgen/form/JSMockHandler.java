package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMockHandler implements JSExpr {
	private final SolidName name;

	public JSMockHandler(SolidName name) {
		this.name = name;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.print("_cxt.mockHandler(new ");
		w.print(name.jsName());
		w.print("(_cxt))");
	}
}
