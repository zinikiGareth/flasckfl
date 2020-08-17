package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

// This is used to obtain the "name" of a variable so it can be looked up in a table of mocks in the JSRunner
// It is not clear that this is even a good idea, but it is inconsistent between JS & JVM and that inconsistency should be addressed
// At the moment (2020-08-12) I would lean toward removing this and making the JS world use actual variables for the mocks; I don't see why it can't 
public class JSNameOf implements IVForm {
	private final JSExpr expr;

	public JSNameOf(JSExpr expr) {
		this.expr = expr;
	}

	@Override
	public String asVar() {
		return "'" + expr.asVar() + "'";
	}

	@Override
	public void write(IndentWriter w) {
		expr.write(w);
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		jvm.local(this, jvm.arg(expr));
	}

	@Override
	public void asivm(IVFWriter iw) {
		iw.print(expr.asVar());
	}
}
