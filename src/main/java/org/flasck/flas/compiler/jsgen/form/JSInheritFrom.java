package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSInheritFrom implements JSExpr {
	private final NameOfThing baseClass;

	public JSInheritFrom(NameOfThing baseClass) {
		this.baseClass = baseClass;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("not a var");
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.println(baseClass.jsName() + ".call(this, _cxt);");
	}

}
