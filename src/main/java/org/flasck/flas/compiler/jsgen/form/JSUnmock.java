package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSUnmock implements JSExpr {
	private final JSExpr mock;

	public JSUnmock(JSExpr mock) {
		this.mock = mock;
	}

	@Override
	public String asVar() {
		return mock.asVar() + ".card";
	}

	@Override
	public void write(IndentWriter w) {
		w.print(asVar());
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr mc = md.callVirtual(J.FLCARD, md.castTo(jvm.argAsIs(mock), J.MOCKCARD), "card");
		jvm.local(this, mc);
	}
}
