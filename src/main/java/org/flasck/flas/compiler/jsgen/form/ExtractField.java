package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.hsi.Slot;
import org.zinutils.bytecode.mock.IndentWriter;

public class ExtractField implements JSExpr {
	private final JSVar asVar;
	private final JSExpr fromVar;
	private final String field;
	private final Slot child;

	public ExtractField(JSVar asVar, JSExpr fromVar, String field, Slot c) {
		this.asVar = asVar;
		this.fromVar = fromVar;
		this.field = field;
		this.child = c;
	}

	@Override
	public String asVar() {
		return asVar.asVar();
	}

	@Override
	public void write(IndentWriter w) {
		w.print("var ");
		w.print(asVar.asVar());
		w.print(" = _cxt.field(");
		w.print(fromVar.asVar());
		w.print(", '");
		w.print(field);
		w.println("');");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		// TODO Auto-generated method stub
		jvm.recordSlot(child, jvm.method().aNull());
		jvm.local(this, null);
	}

}
