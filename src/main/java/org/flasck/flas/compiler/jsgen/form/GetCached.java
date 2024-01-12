package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class GetCached implements JSExpr {
	private final String fnName;

	public GetCached(String fnName) {
		this.fnName = fnName;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("Not a var");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.getSingleton('" + fnName + "')");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr e = md.callInterface(J.OBJECT, jvm.cxt(), "getSingleton", md.stringConst(fnName));
		jvm.local(this, e);
	}
}
