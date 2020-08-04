package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSCurry implements JSExpr {
	private final boolean wantObject;
	private final int required;
	private final JSExpr[] args;

	public JSCurry(boolean wantObject, int required, JSExpr... args) {
		this.wantObject = wantObject;
		this.required = required;
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
		if (wantObject) {
			w.print("_cxt.ocurry(");
			w.print(Integer.toString(required-1));
		} else {
			w.print("_cxt.curry(");
			w.print(Integer.toString(required));
		}
		for (JSExpr e : args) {
			w.print(", ");
			w.print(e.asVar());
		}
		w.print(")");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		// TODO Auto-generated method stub
		
	}
}
