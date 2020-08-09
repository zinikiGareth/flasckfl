package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSIntroducedVar implements JSExpr {
	private final JSVar var;

	public JSIntroducedVar(JSVar var) {
		this.var = var;
	}

	@Override
	public String asVar() {
		return "new BoundVar()";
	}

	@Override
	public void write(IndentWriter w) {
		w.print(asVar());
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr ret = md.makeNew(J.BOUNDVAR);
		if (this.var != null) {
			Var v = md.avar(J.BOUNDVAR, var.asVar());
			jvm.bindVar(this, v);
			jvm.local(var, md.assign(v, ret));
		}
		else
			jvm.local(this, ret);
	}
}