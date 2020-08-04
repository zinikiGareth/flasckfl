package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSPushFunction implements JSExpr {
	private final FunctionName name;
	private final String fn;

	public JSPushFunction(FunctionName name, String fn) {
		this.name = name;
		this.fn = fn;
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.print(fn);
		if (jvm != null)
			jvm.pushFunction(this, name);
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}
}
