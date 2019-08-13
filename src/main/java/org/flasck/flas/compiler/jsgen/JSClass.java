package org.flasck.flas.compiler.jsgen;

public class JSClass implements JSClassCreator {

	public JSClass(JSFile inpkg) {
	}

	@Override
	public JSMethodCreator createMethod(String name) {
		JSMethod meth = new JSMethod();
		return meth;
	}

}
