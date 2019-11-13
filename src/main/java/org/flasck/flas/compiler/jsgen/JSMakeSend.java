package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSMakeSend implements JSExpr {
	private final String sendMeth;
	private final JSExpr obj;
	private final int nargs;
	private String var;

	public JSMakeSend(String sendMeth, JSExpr obj, int nargs) {
		this.sendMeth = sendMeth;
		this.obj = obj;
		this.nargs = nargs;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.mksend(");
		w.print(sendMeth);
		w.print(",");
		w.print(obj.asVar());
		w.print(",");
		w.print(Integer.toString(nargs));
		w.print(")");
	}

	@Override
	public String asVar() {
		return var;
	}
}
