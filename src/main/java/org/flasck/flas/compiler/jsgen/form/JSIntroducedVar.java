package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSIntroducedVar implements JSExpr {
	private final String var;

	public JSIntroducedVar(String var) {
		this.var = var;
	}

	@Override
	public String asVar() {
		return "new BoundVar(" + new JSString(var).asVar() + ")";
	}

	@Override
	public void write(IndentWriter w) {
		w.print(asVar());
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr ret = md.makeNew(J.BOUNDVAR, var == null ? md.castTo(md.aNull(), J.STRING) : md.stringConst(var));
		jvm.local(this, ret);
	}
}