package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.hsi.Slot;
import org.zinutils.bytecode.IExpr;
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
//		NewMethodDefiner md = jvm.method();
//		IExpr ret;
//		if (slot instanceof ArgSlot) {
//			ArgSlot as = (ArgSlot) slot;
//			int pos = as.argpos();
//			ret = md.arrayItem(J.OBJECT, jvm.fargs(), pos/* -ignoreContainer+state.ignoreSpecial */);
//		} else {
//			throw new NotImplementedException("ctor slots");
//		}
		
		IExpr s = jvm.slot(slot);
		if (s instanceof Var)
			jvm.bindVar(slotName, (Var)s);
		jvm.local(this, null);
	}
}
