package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.mock.IndentWriter;

public class ClearRunner implements JSExpr {

	public ClearRunner() {
	}

	@Override
	public void write(IndentWriter w) {
		w.println("runner.clear();");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		IExpr ret = jvm.method().callInterface("void", jvm.helper(), "clearBody", jvm.cxt());
		jvm.local(this, ret);
	}
	
	@Override
	public String asVar() {
		throw new RuntimeException("Not a var");
	}
}
