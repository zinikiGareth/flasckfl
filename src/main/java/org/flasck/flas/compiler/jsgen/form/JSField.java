package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSField implements JSExpr {

	private String fld;

	public JSField(String f) {
		this.fld = f;
	}

	@Override
	public String asVar() {
		return "this." + fld;
	}

	@Override
	public void write(IndentWriter w) {
		w.print(asVar());
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		jvm.local(this, jvm.method().getField(fld));
	}

}
