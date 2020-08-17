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
	private String var;

	public JSMakeSend(String sendMeth, JSExpr obj, int nargs, JSExpr handler) {
		this.sendMeth = sendMeth;
		this.obj = obj;
		this.nargs = nargs;
		this.handler = handler;
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
		w.print(")");
	}

	@Override
	public String asVar() {
		return var;
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		if (!jvm.hasLocal(handler)) {
			if (handler != null)
				handler.generate(jvm);
			else
				jvm.local(handler, md.aNull());
		}
		IExpr mksend = md.callInterface(J.OBJECT, jvm.cxt(), "mksend", md.stringConst(sendMeth), jvm.arg(obj), md.intConst(nargs), jvm.arg(handler));
		jvm.local(this, mksend);
	}
	
	@Override
	public String toString() {
		return "MakeSend[" + sendMeth + "]";
	}
}
