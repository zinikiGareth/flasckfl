package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.hsi.Slot;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.Var;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSHead implements JSExpr {
	private final String var;
	private final Slot slot;

	public JSHead(String var, Slot slot) {
		this.var = var;
		this.slot = slot;
	}

	@Override
	public String asVar() {
		return var;
	}

	@Override
	public void write(IndentWriter w) {
		w.println(var + " = _cxt.head(" + var + ");");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		Var var = md.avar(J.OBJECT, this.var);
		IExpr assign = md.assign(var, md.callInterface(J.OBJECT, jvm.cxt(), "head", jvm.slot(slot)));
		assign.flush();
		jvm.bindVar(this, var);
		jvm.recordSlot(slot, var);

		
		/* I think this is important, and I copied it for that reason, but I think the test at least is dodgy, so where does that info come from?
		if (state.ocret() != null) {
			IExpr extract = md.callInterface("void", state.fcx, "addAll", state.ocmsgs(), md.as(md.callStatic(J.RESPONSE_WITH_MESSAGES, List.class.getName(), "messages", state.fcx, var), J.OBJECT));
			IExpr reassign = md.assign(var, md.callStatic(J.RESPONSE_WITH_MESSAGES, Object.class.getName(), "response", state.fcx, var));
			block.add(md.ifBoolean(md.instanceOf(var, J.RESPONSE_WITH_MESSAGES), md.block(extract, reassign), null));
		}
		*/
	}

}
