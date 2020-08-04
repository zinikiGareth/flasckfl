package org.flasck.flas.compiler.jsgen.creators;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.packaging.JSEnvironment;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSClass implements JSClassCreator {
	private final JSEnvironment jse;
	private final String name;
	private final List<JSMethod> methods = new ArrayList<>();
	private final JSMethod ctor;
	private NameOfThing baseClass;
	
	public JSClass(JSEnvironment jse, String fullName) {
		this.jse = jse;
		this.name = fullName;
		ctor = classMethod(null);
		this.ctor.argument("_cxt");
	}

	public String name() {
		return name;
	}

	@Override
	public void inheritsFrom(NameOfThing baseClass) {
		this.baseClass = baseClass;
		this.ctor.inheritFrom(baseClass);
	}
	
	@Override
	public void arg(String a) {
		this.ctor.argument(a);
	}

	@Override
	public JSMethodCreator createMethod(String name, boolean prototype) {
		JSMethod meth = new JSMethod(jse, null, this.name, prototype, name);
		methods.add(meth);
		return meth;
	}
	
	public JSMethod classMethod(String mname) {
		return new JSMethod(jse, null, this.name, false, mname);
	}

	@Override
	public JSMethodCreator constructor() {
		return ctor;
	}

	public void writeTo(IndentWriter iw) {
		ctor.write(iw);
		if (this.baseClass != null) {
			iw.println(name + ".prototype = new " + this.baseClass.jsName() + "();");
			iw.println(name + ".prototype.constructor = " + name + ";");
		}
		for (JSMethod m : methods)
			m.write(iw);
	}

	public void generate(ByteCodeEnvironment bce) {
		ctor.generate(bce);
		for (JSMethod m : methods)
			m.generate(bce);
	}
}
