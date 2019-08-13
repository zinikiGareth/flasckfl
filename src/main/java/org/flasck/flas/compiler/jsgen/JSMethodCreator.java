package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public interface JSMethodCreator {
	JSExpr literal(String text);
	JSExpr string(String string);
	JSExpr argument(String name);
	JSExpr callMethod(JSExpr obj, String meth, JSExpr... args);
	void write(IndentWriter w);
	JSExpr callStatic(String clz, String meth, JSExpr... args);
}
