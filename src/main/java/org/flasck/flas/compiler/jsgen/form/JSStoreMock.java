package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
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
	public void write(IndentWriter w) {
		w.print("_cxt.storeMock('");
		w.print(nameAs.asVar());
		w.print("', ");
		w.print(value.asVar());
		w.print(")");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr ret = md.callInterface(J.OBJECT, jvm.cxt(), "storeMock", jvm.arg(value));
		jvm.local(this, ret);
	}

}
