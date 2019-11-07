package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.names.SolidName;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMockContract implements JSExpr {
	private final JSMethod meth;
	private String var;
	private final SolidName name;

	public JSMockContract(JSMethod meth, SolidName name) {
		this.meth = meth;
		this.name = name;
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
		w.print(" = _cxt.mockContract(");
		w.print(name.jsName()); // TODO: should this be JSPName or JSCName or something?
		w.println(");");
	}
}
