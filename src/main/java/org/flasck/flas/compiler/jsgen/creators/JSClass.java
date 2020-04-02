package org.flasck.flas.compiler.jsgen.creators;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSClass implements JSClassCreator {
	private final String name;
	private final List<JSMethod> methods = new ArrayList<>();
	private final JSMethod ctor;
	
	public JSClass(String fullName) {
		this.name = fullName;
		ctor = classMethod(null);
		this.ctor.argument("_cxt");
	}

	@Override
	public void arg(String a) {
		this.ctor.argument(a);
	}

	@Override
	public JSMethodCreator createMethod(String name, boolean prototype) {
		JSMethod meth = new JSMethod(this.name, prototype, name);
		methods.add(meth);
		return meth;
	}
	
	public JSMethod classMethod(String mname) {
		return new JSMethod(this.name, false, mname);
	}

	@Override
	public JSBlockCreator constructor() {
		return ctor;
	}

	public void writeTo(IndentWriter iw) {
		/*
		iw.println("");
		iw.print(name);
		iw.print(" = function(");
		iw.print(String.join(", ", ctorArgs ));
		iw.print(") ");
		ctor.write(iw);
		iw.println("");
		*/
		ctor.write(iw);
		for (JSMethod m : methods)
			m.write(iw);
	}

}
