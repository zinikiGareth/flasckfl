package org.flasck.flas.compiler.jsgen.creators;

import java.util.List;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.JSFunctionState.StateLocation;
import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSIfExpr;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.TupleMember;
import org.zinutils.bytecode.mock.IndentWriter;

public interface JSBlockCreator {
	// very simple and obvious things
	JSExpr literal(String text);
	JSExpr string(String string);
	JSExpr newOf(NameOfThing clz);
	JSExpr newOf(NameOfThing clz, List<JSExpr> args);
	JSExpr makeArray(JSExpr... args);
	JSExpr jsArray(Iterable<JSExpr> arr);
	JSExpr makeTuple(JSExpr... args);
	JSExpr callMethod(JSExpr obj, String meth, JSExpr... args);
	JSExpr cxtMethod(String meth, JSExpr... args);
	JSExpr pushFunction(String meth);
	JSExpr pushConstructor(String clz);
	JSExpr defineTupleMember(TupleMember e);

	// creating more complex things
	JSExpr structConst(String name);
	JSExpr mockContract(SolidName name);
	JSExpr mockHandler(SolidName name);
	JSExpr createObject(NameOfThing name);
	JSExpr createObject(NameOfThing name, List<JSExpr> args);
	JSExpr createObject(String name, List<JSExpr> args);
	JSExpr createAgent(CardName cardName);
	JSExpr makeSend(String sendMeth, JSExpr obj, int nargs, JSExpr handler);
	JSExpr makeAcor(String acorMeth, JSExpr obj, int nargs);
	JSExpr introduceVar(String var);
	JSExpr fromIntroduction(JSExpr boundVar);
	void recordContract(String ctr, String impl);
	void requireContract(String referAsVar, String jsName);
	
	// create an object of clz and store in a field
	JSExpr fieldObject(String field, String clz);
	void stateField();
	void setField(String field, JSExpr expr);
	JSExpr fromCard();
	JSVar arg(int pos);

	// Stored values
	JSExpr boundVar(String var);
	JSExpr tupleMember(FunctionName name);
	JSExpr lambda(HandlerLambda defn);
	JSExpr member(String var);

	// HSIE logic statements
	void bindVar(String slot, String var);
	void head(String var);
	void field(String asVar, String fromVar, String field);
	JSIfExpr ifCtor(String var, String ctor);
	JSIfExpr ifConst(String string, int cnst);
	JSIfExpr ifConst(String string, String cnst);
	JSIfExpr ifTrue(JSExpr ge);
	void errorNoCase();
	void errorNoDefaultGuard();
	void error(JSExpr msg);

	// main logic statements
	void storeField(JSExpr inObj, String field, JSExpr value);
	JSExpr loadField(StateLocation loc, String name);
	JSExpr contractByVar(StateLocation loc, String name);
	JSExpr structArgs(String string, JSExpr... args);
	JSExpr closure(JSExpr... args);
	JSExpr curry(int expArgs, JSExpr... args);
	JSExpr xcurry(int expArgs, List<XCArg> posargs);
	void returnObject(JSExpr jsExpr);

	// unit testing
	void assertable(JSExpr runner, String assertion, JSExpr... args);
	void expect(JSExpr mock, String var, List<JSExpr> args, JSExpr handler);
	void assertSatisfied(String var);

	// Send the block to disk
	void write(IndentWriter w);
}