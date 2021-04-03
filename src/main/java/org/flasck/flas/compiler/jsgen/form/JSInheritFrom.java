package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JSMethod;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSInheritFrom implements JSExpr {
	private final JSMethod meth;
	private final NameOfThing baseClass;

	public JSInheritFrom(JSMethod meth, NameOfThing baseClass) {
		this.meth = meth;
		this.baseClass = baseClass;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("not a var");
	}

	@Override
	public void write(IndentWriter w) {
		w.print(baseClass.jsName() + ".call(this");
		for (JSExpr x : meth.superArgs) {
			w.print(", " + x.asVar());
		}
		w.println(");");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		jvm.local(this, null);
	}
}
