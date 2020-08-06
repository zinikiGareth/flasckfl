package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.hsi.Slot;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSBind implements JSExpr {
	private final Slot slot;
	private final JSExpr slotName;
	private final String var;

	public JSBind(Slot slot, JSExpr slotName, String var) {
		this.slot = slot;
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
		IExpr s = jvm.slot(slot);
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
