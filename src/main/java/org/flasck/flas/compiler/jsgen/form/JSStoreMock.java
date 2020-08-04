package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSStoreMock implements JSExpr {
	private final JSExpr value;
	private JSLocal nameAs;

	public JSStoreMock(JSExpr value) {
		this.value = value;
	}
	
	public void nameAs(JSLocal ret) {
		nameAs = ret;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("Store in a local");
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.print("_cxt.storeMock('");
		w.print(nameAs.asVar());
		w.print("', ");
		w.print(value.asVar());
		w.print(")");
	}

}
