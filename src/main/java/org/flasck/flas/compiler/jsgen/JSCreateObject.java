package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.names.SolidName;
import org.zinutils.bytecode.mock.IndentWriter;

// I think this and JSStruct are basically the same
public class JSCreateObject implements JSExpr {
	private final String clz;

	public JSCreateObject(SolidName name) {
		this.clz = name.jsName();
	}

	public JSCreateObject(String clz) {
		this.clz = clz;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print(clz);
		w.print(".eval(_cxt)");
	}
}
