package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMakeSend implements JSExpr {
	private final String sendMeth;
	private final JSExpr obj;
	private final int nargs;
	private final JSExpr handler;
	private final JSExpr handlerName;
	private String var;

	public JSMakeSend(String sendMeth, JSExpr obj, int nargs, JSExpr handler, JSExpr handlerName) {
		this.sendMeth = sendMeth;
		this.obj = obj;
		this.nargs = nargs;
		this.handler = handler;
		this.handlerName = handlerName;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.mksend(");
		w.print(new JSString(sendMeth).asVar());
		w.print(",");
		w.print(obj.asVar());
		w.print(",");
		w.print(Integer.toString(nargs));
		w.print(",");
		if (handler != null) {
			w.print(handler.asVar());
		} else
			w.print("null");
		w.print(",");
		if (handlerName != null) {
			w.print(handlerName.asVar());
		} else
			w.print("null");
		w.print(")");
	}

	@Override
	public String asVar() {
		return var;
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr h;
		if (handler != null)
			h = jvm.arg(handler);
		else
			h = md.aNull();
		IExpr sn;
		if (handlerName != null)
			sn = jvm.arg(handlerName);
		else
			sn = md.aNull();
		IExpr mksend = md.callInterface(J.OBJECT, jvm.cxt(), "mksend", md.stringConst(sendMeth), jvm.arg(obj), md.intConst(nargs), h, sn);
		jvm.local(this, mksend);
	}
	
	@Override
	public String toString() {
		return "MakeSend[" + sendMeth + "]";
	}
}
