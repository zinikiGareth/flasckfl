package org.flasck.flas.compiler.jsgen.creators;

import java.util.List;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSIfExpr;
import org.zinutils.bytecode.mock.IndentWriter;

public interface JSBlockCreator {
	// very simple and obvious things
	JSExpr literal(String text);
	JSExpr string(String string);
	JSExpr newOf(SolidName clz);
	JSExpr makeArray(JSExpr... args);
	JSExpr callMethod(JSExpr obj, String meth, JSExpr... args);
	JSExpr pushFunction(String meth);
	
	// creating more complex things
	JSExpr structConst(String name);
	JSExpr mockContract(SolidName name);
	JSExpr createObject(SolidName name);
	JSExpr makeSend(String sendMeth, JSExpr obj, int nargs);
	JSExpr makeAcor(String acorMeth, JSExpr obj, int nargs);
	
	// create an object of clz and store in a field
	JSExpr fieldObject(String field, String clz);

	// HSIE logic statements
	JSExpr boundVar(String var);
	void bindVar(String slot, String var);
	void head(String var);
	void field(String asVar, String fromVar, String field);
	JSIfExpr ifCtor(String var, String ctor);
	JSIfExpr ifConst(String string, int cnst);
	JSIfExpr ifConst(String string, String cnst);
	JSIfExpr ifTrue(JSExpr ge);
	void errorNoCase();
	void errorNoDefaultGuard();

	// main logic statements
	void storeField(JSExpr inObj, String field, JSExpr value);
	JSExpr loadField(String name);
	JSExpr structArgs(String string, JSExpr... args);
	JSExpr closure(JSExpr... args);
	JSExpr curry(int expArgs, JSExpr... args);
	JSExpr xcurry(int expArgs, List<XCArg> posargs);
	void returnObject(JSExpr jsExpr);

	// unit testing
	void assertable(JSExpr runner, String assertion, JSExpr... args);
	
	// Send the block to disk
	void write(IndentWriter w);
}