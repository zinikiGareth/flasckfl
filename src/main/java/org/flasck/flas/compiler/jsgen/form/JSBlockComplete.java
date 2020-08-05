package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSBlockComplete implements JSExpr {

	@Override
	public String asVar() {
		return null;
	}

	@Override
	public void write(IndentWriter w) {
		w.println("runner.checkAtEnd()");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr tc = md.callInterface("void", jvm.helper(), "testComplete");
		IExpr rv = md.returnVoid();

		jvm.local(this, md.block(tc, rv));
	}

}
