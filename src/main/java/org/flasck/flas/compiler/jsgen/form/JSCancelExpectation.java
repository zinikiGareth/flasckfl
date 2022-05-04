package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSCancelExpectation implements JSExpr {
	private final JSExpr mock;

	public JSCancelExpectation(JSExpr mock) {
		this.mock = mock;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException();
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.expectCancel(");
		w.print(mock.asVar());
		w.println(");");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
//		IExpr mk = md.castTo(jvm.argAsIs(mock), J.EXPECTING);
		IExpr mk = jvm.arg(mock);
		IExpr x = md.callInterface("void", md.castTo(jvm.cxt(), J.CARDCONTEXT), "expectCancel", mk);
		jvm.local(this, x);
	}

}
