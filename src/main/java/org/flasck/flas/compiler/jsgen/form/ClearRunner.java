package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class ClearRunner implements JSExpr {

	public ClearRunner() {
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.println("runner.clear();");
		
		if (jvm != null) {
			jvm.method().callInterface("void", jvm.helper(), "clearBody", jvm.cxt()).flush();
		}
	}

	@Override
	public String asVar() {
		throw new RuntimeException("Not a var");
	}
}
