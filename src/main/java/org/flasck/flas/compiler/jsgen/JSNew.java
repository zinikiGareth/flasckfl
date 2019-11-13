package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.names.SolidName;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSNew implements JSExpr {
	private final SolidName clz;

	public JSNew(SolidName clz) {
		this.clz = clz;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("new ");
		w.print(clz.jsName()); // TODO: should this be JSPName or JSCName or something?
		w.print("()");
	}

}
