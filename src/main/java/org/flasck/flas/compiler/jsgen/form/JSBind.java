package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.hsi.Slot;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSBind implements JSExpr {
	private final Slot slot;
	private final String slotName;
	private final String var;

	public JSBind(Slot slot, String slotName, String var) {
		this.slot = slot;
		this.slotName = slotName;
		this.var = var;
	}

	@Override
	public String asVar() {
		return var;
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.println("const " + var + " = " + slotName + ";");
		if (jvm != null)
			jvm.bind(this, slot);
	}

}
