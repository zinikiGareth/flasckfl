package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.names.SolidName;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMockContract implements JSExpr {
	private final SolidName name;

	public JSMockContract(SolidName name) {
		this.name = name;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.mockContract(new ");
		w.print(name.jsName()); // TODO: should this be JSPName or JSCName or something?
		w.print("())");
	}
}
