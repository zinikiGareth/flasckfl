package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSMakeSend implements JSExpr {
	private final JSMethod meth;
	private final String sendMeth;
	private final JSExpr obj;
	private final int nargs;
	private String var;

	public JSMakeSend(JSMethod meth, String sendMeth, JSExpr obj, int nargs) {
		this.meth = meth;
		this.sendMeth = sendMeth;
		this.obj = obj;
		this.nargs = nargs;
	}

	@Override
	public void write(IndentWriter w) {
		if (var == null)
			var = meth.obtainNextVar();
		w.print("const " + var + " = ");
		w.print("_cxt.mksend(");
		w.print(sendMeth);
		w.print(",");
		w.print(obj.asVar());
		w.print(",");
		w.print(Integer.toString(nargs));
		w.println(");");
	}

	@Override
	public String asVar() {
		if (var == null)
			var = meth.obtainNextVar();
		return var;
	}
}
