package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSSplitRWM implements JSExpr {
	private final JSExpr ocmsgs;
	private final JSExpr var;

	public JSSplitRWM(JSExpr ocmsgs, JSExpr var) {
		this.ocmsgs = ocmsgs;
		this.var = var;
	}

	@Override
	public String asVar() {
		return var.asVar();
	}

	@Override
	public void write(IndentWriter w) {
		w.println("if (" + var.asVar() + " instanceof ResponseWithMessages) {");
		IndentWriter iw = w.indent();
		iw.println("_cxt.addAll(" + ocmsgs.asVar() + ", ResponseWithMessages.messages(_cxt, " + var.asVar() + "));");
		iw.println(var.asVar() + " = ResponseWithMessages.response(_cxt, " + var.asVar() + ");");
		w.println("}");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr then = md.voidExpr(md.aNull());
		IExpr ib = md.ifBoolean(md.instanceOf(jvm.arg(var), J.RESPONSE_WITH_MESSAGES), then, null);
		jvm.local(this, ib);
	}
}
