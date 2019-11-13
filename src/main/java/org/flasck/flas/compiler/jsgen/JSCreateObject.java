package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.names.SolidName;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSCreateObject implements JSExpr {
	private final SolidName name;

	public JSCreateObject(SolidName name) {
		this.name = name;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print(name.jsName());
		w.print(".eval(_cxt)");
	}
}
