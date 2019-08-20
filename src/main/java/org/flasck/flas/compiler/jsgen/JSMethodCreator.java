package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public interface JSMethodCreator {
	JSExpr literal(String text);
	JSExpr string(String string);
	JSExpr argument(String name);
	JSExpr callFunction(String string, JSExpr... args);
	JSExpr callMethod(JSExpr obj, String meth, JSExpr... args);
	JSExpr pushFunction(String meth);
	JSExpr closure(JSExpr... args);
	JSExpr curry(int expArgs, JSExpr... args);
	void returnObject(JSExpr jsExpr);
	void assertable(JSExpr runner, String assertion, JSExpr... args);

	void write(IndentWriter w);
}
