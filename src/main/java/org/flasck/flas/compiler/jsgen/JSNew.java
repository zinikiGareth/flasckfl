package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.names.SolidName;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSNew implements JSExpr {
	private final JSMethod meth;
	private final SolidName clz;
	private String var;

	public JSNew(JSMethod meth, SolidName clz) {
		this.meth = meth;
		this.clz = clz;
	}

	@Override
	public String asVar() {
		if (var == null)
			var = meth.obtainNextVar();
		return var;
	}

	@Override
	public void write(IndentWriter w) {
		if (var == null)
			var = meth.obtainNextVar();
		w.print("const ");
		w.print(var);
		w.print(" = new ");
		w.print(clz.jsName()); // TODO: should this be JSPName or JSCName or something?
		w.println("();");
	}

}
