package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSCompare implements JSExpr {
	private final JSExpr lhs;
	private final JSExpr rhs;

	public JSCompare(JSExpr lhs, JSExpr rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}

	@Override
	public String asVar() {
		return lhs.asVar() + " == " + rhs.asVar();
	}

	@Override
	public void write(IndentWriter w) {
		w.print(asVar());
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr ret = md.callVirtual("boolean", jvm.arg(lhs), "equals", jvm.arg(rhs));
		jvm.local(this, ret);
	}

}
