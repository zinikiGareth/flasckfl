package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSWillSplitRWM implements JSExpr {
	private final JSExpr var;
	private final JSExpr ocmsgs;

	public JSWillSplitRWM(JSExpr r, JSExpr ocmsgs) {
		this.var = r;
		this.ocmsgs = ocmsgs;
	}

	@Override
	public String asVar() {
		return var.asVar();
	}

	@Override
	public void write(IndentWriter w) {
		w.println(var.asVar() + ".splitRWM(" + ocmsgs.asVar() + ");");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr cv = md.callVirtual("void", jvm.argAsIs(var), "splitRWM", jvm.argAsIs(ocmsgs));
		jvm.local(this, cv);
	}
}
