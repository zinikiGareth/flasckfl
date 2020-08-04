package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMakeAcor implements JSExpr {
	private final String acorMeth;
	private final JSExpr obj;
	private final int nargs;
	private String var;

	public JSMakeAcor(String acorMeth, JSExpr obj, int nargs) {
		this.acorMeth = acorMeth;
		this.obj = obj;
		this.nargs = nargs;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.mkacor(");
		w.print(acorMeth);
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

	@Override
	public void generate(JVMCreationContext jvm) {
		// TODO Auto-generated method stub
		
	}
}
