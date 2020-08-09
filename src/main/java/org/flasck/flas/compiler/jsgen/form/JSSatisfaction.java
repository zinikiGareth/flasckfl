package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSSatisfaction implements JSExpr {
	private final JSExpr expr;

	public JSSatisfaction(JSExpr expr) {
		this.expr = expr;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException();
	}

	@Override
	public void write(IndentWriter w) {
		w.print(expr.asVar());
		w.println(".assertSatisfied(_cxt);");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner meth = jvm.method();
		if (!jvm.hasLocal(expr))
			expr.generate(jvm);
		IExpr ret = meth.callInterface("void", meth.castTo(jvm.arg(expr), J.EXPECTING), "assertSatisfied", jvm.cxt());
		jvm.local(this, ret);
	}

}
