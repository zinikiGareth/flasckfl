package org.flasck.flas.compiler.jsgen;

public interface JSMethodCreator {
	JSExpr literal(String text);
	JSExpr argument(String name);
	JSExpr callMethod(JSExpr obj, String meth, JSExpr... args);
}
