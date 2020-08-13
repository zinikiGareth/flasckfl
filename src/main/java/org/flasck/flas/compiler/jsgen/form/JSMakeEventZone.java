package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMakeEventZone implements JSExpr {
	private final JSExpr type;
	private final JSExpr expr;

	public JSMakeEventZone(JSExpr type, JSExpr expr) {
		this.type = type;
		this.expr = expr;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.array(");
		w.print(type.asVar());
		w.print(", ");
		w.print(expr.asVar());
		w.print(")");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner meth = jvm.method();
		JSString ty = (JSString) type;
		if (!jvm.hasLocal(expr))
			expr.generate(jvm);
		IExpr ez = meth.makeNew(J.EVENTZONE, meth.stringConst(ty.value()), jvm.arg(expr));
		jvm.local(this, ez);
	}

}
