package org.flasck.flas.compiler.jsgen;

import org.zinutils.bytecode.mock.IndentWriter;

public interface JSBlockCreator {
	JSExpr literal(String text);
	JSExpr string(String string);
	JSExpr structConst(String name);
	void bindVar(String slot, String var);
	void head(String var);
	JSIfExpr ifCtor(String var, String ctor);
	void errorNoCase();
	JSExpr callFunction(String string, JSExpr... args);
	JSExpr callMethod(JSExpr obj, String meth, JSExpr... args);
	JSExpr pushFunction(String meth);
	JSExpr closure(JSExpr... args);
	JSExpr curry(int expArgs, JSExpr... args);
	void returnObject(JSExpr jsExpr);
	void assertable(JSExpr runner, String assertion, JSExpr... args);
	JSExpr boundVar(String var);

	void write(IndentWriter w);
}
