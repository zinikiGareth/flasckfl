package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSClass implements JSClassCreator {
	private final String name;
	private final List<JSMethod> methods = new ArrayList<>();

	public JSClass(String fullName) {
		this.name = fullName;
	}

	@Override
	public JSMethodCreator createMethod(String name, boolean prototype) {
		JSMethod meth = new JSMethod(this.name, prototype, name);
		methods.add(meth);
		return meth;
	}

	public void writeTo(IndentWriter iw) {
		iw.println("");
		iw.print(name);
		iw.println(" = function() {");
		iw.println("}");
		for (JSMethod m : methods)
			m.write(iw);
	}

}
