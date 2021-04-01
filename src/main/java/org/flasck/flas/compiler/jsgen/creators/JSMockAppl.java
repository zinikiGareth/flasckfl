package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSMockAppl implements JSExpr {
	private final JSExpr runner;

	public JSMockAppl(JSExpr runner) {
		this.runner = runner;
	}

	@Override
	public String asVar() {
		return runner.asVar() + ".newMockAppl(_cxt)";
	}

	@Override
	public void write(IndentWriter w) {
		throw new NotImplementedException("should be assigned directly");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		IExpr r = jvm.argAsIs(runner);
		jvm.local(this, jvm.method().callInterface(J.MOCKAPPL, r, "newMockAppl", jvm.cxt()));
	}

}
