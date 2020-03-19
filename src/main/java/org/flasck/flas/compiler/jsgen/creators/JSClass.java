package org.flasck.flas.compiler.jsgen.creators;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSClass implements JSClassCreator {
	private final String name;
	private final List<String> ctorArgs = new ArrayList<>();
	private final List<JSMethod> methods = new ArrayList<>();
	private final JSBlock ctorBlock = JSBlock.classMethod(this);
	
	public JSClass(String fullName) {
		this.name = fullName;
		this.ctorArgs.add("_cxt");
	}

	@Override
	public void arg(String a) {
		this.ctorArgs.add(a);
	}

	@Override
	public JSMethodCreator createMethod(String name, boolean prototype) {
		JSMethod meth = new JSMethod(this.name, prototype, name);
		methods.add(meth);
		return meth;
	}

	@Override
	public JSBlockCreator constructor() {
		return ctorBlock;
	}

	public void writeTo(IndentWriter iw) {
		iw.println("");
		iw.print(name);
		iw.print(" = function(");
		iw.print(String.join(", ", ctorArgs ));
		iw.print(") ");
		ctorBlock.write(iw);
		iw.println("");
		for (JSMethod m : methods)
			m.write(iw);
	}

}
