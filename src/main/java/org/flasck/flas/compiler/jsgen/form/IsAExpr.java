package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class IsAExpr implements JSExpr {
	private final JSExpr var;
	private final String ctor;

	public IsAExpr(JSExpr var, NameOfThing ctor) {
		this.var = var;
		this.ctor = ctor.jsName();
	}

	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.isA(" + var.asVar() + ", '" + ctor + "')");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr myVar = jvm.arg(var);
		IExpr ret = md.callInterface(J.BOOLEANP.getActual(), jvm.cxt(), "isA", myVar , md.stringConst(ctor));
		jvm.local(this, ret);
	}
}
