package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSClass implements JSClassCreator {
	private final String pkg;
	private final String name;

	public JSClass(String pkg, String name) {
		this.pkg = pkg;
		this.name = name;
	}

	@Override
	public JSMethodCreator createMethod(String name) {
		JSMethod meth = new JSMethod(pkg + "." + this.name, name);
		return meth;
	}

	public void writeTo(IndentWriter iw) {
		iw.println("");
		iw.print(pkg);
		iw.print(".");
		iw.print(name);
		iw.println(" = function() {");
		iw.println("}");
	}

}
