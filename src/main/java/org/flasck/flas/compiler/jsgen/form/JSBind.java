package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

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
	public void write(IndentWriter w) {
		w.println("const " + var + " = " + slotName + ";");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		if (slot instanceof ArgSlot) {
			ArgSlot as = (ArgSlot) slot;
			int pos = as.argpos();
			
		} else {
			throw new NotImplementedException("ctor slots");
		}
	}
}
