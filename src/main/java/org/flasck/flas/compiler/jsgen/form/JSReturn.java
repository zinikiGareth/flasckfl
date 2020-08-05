package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSReturn implements JSExpr {
	private final JSExpr jsExpr;

	public JSReturn(JSExpr jsExpr) {
		this.jsExpr = jsExpr;
	}

	@Override
	public String asVar() {
		return null;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("return ");
		w.print(jsExpr.asVar());
		w.println(";");
	}
	
	@Override
	public void generate(JVMCreationContext jvm) {
		IExpr ret = jvm.method().returnObject(jvm.arg(jsExpr));
		jvm.local(this, ret);
	}
}
