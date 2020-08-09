package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSNewState implements JSExpr {

	public JSNewState() {
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.println("this.state = _cxt.fields();");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		jvm.local(this, null);
	}
}
