package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSFromCard implements JSExpr {

	@Override
	public String asVar() {
		return "this._card";
	}

	@Override
	public void write(IndentWriter w) {
		// shouldn't happen
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		jvm.local(this, jvm.method().getField("_card"));
	}

}
