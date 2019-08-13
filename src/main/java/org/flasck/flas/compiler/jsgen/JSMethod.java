package org.flasck.flas.compiler.jsgen;

public class JSMethod implements JSMethodCreator {

	// handling quotes for strings - would it be better to separate strings out?
	@Override
	public JSExpr literal(String text) {
		return new JSLiteral(text);
	}

	@Override
	public JSExpr argument(String name) {
		return new JSVar(name);
	}

	@Override
	public JSExpr callMethod(JSExpr obj, String meth, JSExpr... args) {
		return new JSCall(obj, meth, args);
	}

}
