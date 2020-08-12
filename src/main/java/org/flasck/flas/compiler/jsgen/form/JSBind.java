package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSBind implements JSExpr {
	private final JSExpr slotName;
	private final String var;

	public JSBind(JSExpr slotName, String var) {
		this.slotName = slotName;
		this.var = var;
	}

	@Override
	public String asVar() {
		return var;
	}

	@Override
	public void write(IndentWriter w) {
		w.println("const " + var + " = " + slotName.asVar() + ";");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		IExpr s = jvm.arg(slotName);
		Var v;
		if (!(s instanceof Var)) {
			NewMethodDefiner md = jvm.method();
			v = md.avar(J.OBJECT, this.var);
			jvm.bindVar(slotName, v);
			jvm.local(this, md.assign(v, s));
		} else {
			v = (Var) s;
			jvm.local(this, null);
		}
		jvm.bindVar(new JSVar(var), v);
	}
}
