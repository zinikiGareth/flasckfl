package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSError implements JSExpr {
	private final JSExpr msg;

	public JSError(String msg) {
		this.msg = new JSString(msg);
	}

	public JSError(JSExpr msg) {
		this.msg = msg; 
	}

	@Override
	public String asVar() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void write(IndentWriter w) {
		w.println("return FLError.eval(_cxt, " + msg.asVar() + ");");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		// TODO Auto-generated method stub
		
	}
}
