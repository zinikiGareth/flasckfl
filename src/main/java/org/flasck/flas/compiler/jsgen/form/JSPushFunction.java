package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSPushFunction implements JSExpr {
	private final String fn;

	public JSPushFunction(String fn) {
		this.fn = fn;
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.print(fn);
		if (jvm != null)
			jvm.pushFunction(fn);
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}
}
