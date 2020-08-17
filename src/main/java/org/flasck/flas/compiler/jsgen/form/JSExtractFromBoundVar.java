package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSExtractFromBoundVar implements JSExpr {
	private final JSExpr boundVar;

	public JSExtractFromBoundVar(JSExpr boundVar) {
		this.boundVar = boundVar;
	}

	@Override
	public String asVar() {
		return this.boundVar.asVar() + ".introduced()";
	}

	@Override
	public void write(IndentWriter w) {
		throw new NotImplementedException();
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		jvm.local(this, jvm.method().callVirtual(J.OBJECT, jvm.argAsIs(boundVar), "introduced"));
	}

}
