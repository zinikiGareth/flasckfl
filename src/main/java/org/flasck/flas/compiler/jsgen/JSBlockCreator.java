package org.flasck.flas.compiler.jsgen;

import java.util.List;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.zinutils.bytecode.mock.IndentWriter;

public interface JSBlockCreator {
	JSExpr literal(String text);
	JSExpr string(String string);
	JSExpr makeArray(JSExpr... args);
	JSExpr structConst(String name);
	void bindVar(String slot, String var);
	void head(String var);
	void field(String asVar, String fromVar, String field);
	JSIfExpr ifCtor(String var, String ctor);
	JSIfExpr ifConst(String string, int cnst);
	JSIfExpr ifConst(String string, String cnst);
	JSIfExpr ifTrue(JSExpr ge);
	void errorNoCase();
	void errorNoDefaultGuard();
	JSExpr structArgs(String string, JSExpr... args);
	JSExpr callMethod(JSExpr obj, String meth, JSExpr... args);
	JSExpr pushFunction(String meth);
	JSExpr closure(JSExpr... args);
	JSExpr curry(int expArgs, JSExpr... args);
	JSExpr xcurry(int expArgs, List<XCArg> posargs);
	void returnObject(JSExpr jsExpr);
	void assertable(JSExpr runner, String assertion, JSExpr... args);
	JSExpr boundVar(String var);
	JSExpr mockContract(SolidName name);
	JSExpr makeSend(String sendMeth, JSExpr obj, int nargs);

	void write(IndentWriter w);
}
