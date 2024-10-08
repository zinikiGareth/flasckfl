package org.flasck.flas.compiler.jsgen.creators;

import java.util.List;

import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.compiler.jsgen.JSStyleIf;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.tc3.NamedType;
import org.zinutils.bytecode.mock.IndentWriter;

public interface JSBlockCreator {
	// very simple and obvious things
	JSExpr literal(String text);
	JSString string(String string);
	JSExpr newOf(NameOfThing clz);
	JSExpr newOf(NameOfThing clz, List<JSExpr> args);
	JSExpr makeArray(JSExpr... args);
	JSExpr makeArray(List<JSExpr> args);
	JSExpr makeHash(List<JSExpr> stack);
	JSExpr applyHash(JSExpr basic, JSExpr hash);
	JSExpr makeEventZone(JSExpr string, JSExpr je);
	JSExpr jsArray(List<JSExpr> arr);
	JSExpr arrayElt(JSExpr tc, int i);
	JSExpr makeTuple(JSExpr... args);
	JSExpr callStatic(NameOfThing clz, int nargs);
	JSExpr callMethod(String returnType, JSExpr obj, String method, JSExpr... args);
	JSExpr cxtMethod(String meth, JSExpr... args);
	JSExpr pushFunction(String meth, FunctionName name, int argcount);
	JSExpr pushConstructor(NameOfThing name, String clz);
	JSExpr defineTupleMember(TupleMember e);
	void returnVoid();
	void returnObject(JSExpr jsExpr);
	void returnCompare(JSExpr lhs, JSExpr rhs);

	// creating more complex things
	JSExpr structConst(NameOfThing name);
	JSExpr mockContract(SolidName name);
	JSExpr mockHandler(SolidName name);
	JSExpr createObject(NameOfThing name);
	JSExpr createObject(NameOfThing name, List<JSExpr> args);
	JSExpr makeSend(String sendMeth, JSExpr obj, int nargs, JSExpr handler, JSExpr handlerName);
	JSExpr makeAcor(FunctionName acorMeth, JSExpr obj, int nargs);
	JSExpr introduceVar(String var);
	JSExpr fromIntroduction(JSExpr boundVar);
	void recordContract(NameOfThing ctr, CSName impl);
	void requireContract(String referAsVar, NameOfThing impl);
	
	// create an object of clz and store in a field
	JSExpr fieldObject(String field, NameOfThing clz);
	void stateField(boolean jsOnly);
	void setField(boolean jsOnly, String field, JSExpr value);
	void setField(JSExpr obj, String field, JSExpr value);
	JSExpr field(String f);
	JSExpr fromCard(NameOfThing name);
	JSVar arg(int pos);

	// Stored values
	JSExpr boundVar(String var);
	JSExpr tupleMember(FunctionName name);
	JSExpr lambda(HandlerLambda defn);
	JSExpr member(NameOfThing type, String var);

	// HSIE logic statements
	void bindVar(Slot slot, JSExpr jsExpr, String var);
	void head(JSExpr currentVar);
	void splitRWM(JSExpr ocmsgs, JSExpr currentVar);
	void willSplitRWM(JSExpr r, JSExpr ocmsgs);
	void keepMessages(JSExpr ocmsgs, JSExpr r);
	void field(JSVar jv, JSExpr jsExpr, String field);
	JSIfCreator ifCtor(JSExpr var, NameOfThing type);
	JSIfCreator ifConst(JSExpr var, int cnst);
	JSIfCreator ifConst(JSExpr var, String cnst);
	JSIfCreator ifTrue(JSExpr ge);
	void errorNoCase();
	void errorNoDefaultGuard();
	void error(JSExpr msg);
	JSExpr checkType(NamedType type, JSExpr res);

	// main logic statements
	void storeField(boolean jsOnly, JSExpr inObj, String field, JSExpr value);
	JSExpr loadField(JSExpr container, String name);
	JSExpr contractByVar(JSExpr container, String name);
	JSExpr structArgs(NameOfThing fn, JSExpr... args);
	JSExpr closure(boolean wantObject, JSExpr... args);
	JSExpr curry(boolean wantObject, int expArgs, JSExpr... args);
	JSExpr xcurry(boolean wantObject, int expArgs, List<XCArg> posargs);

	// templates
	void updateContent(String templateName, TemplateField field, int option, JSExpr source, String fromField, JSExpr expr);
	void updateStyle(String templateName, TemplateField field, int option, JSExpr source, JSExpr constant, List<JSStyleIf> styles);
	void updateTemplate(TemplateField field, int posn, boolean isOtherObject, String templateName, JSExpr expr, JSExpr tc);
	void updateContainer(TemplateField field, JSExpr expr, int ucidx);
	void updatePunnet(TemplateField field, JSExpr expr, int ucidx);
	void addItem(int position, String templateName, JSExpr expr, JSExpr makeArray);
	
	// unit testing
	void assertable(JSExpr runner, String assertion, JSExpr... args);
	void renderObject(JSExpr runner, JSExpr obj, SolidName on, int which, JSString templateName);
	void expect(JSExpr mock, String var, List<JSExpr> args, JSExpr handler);
	void expectCancel(JSExpr mock);
	JSExpr storeMockObject(UnitDataDeclaration udd, JSExpr value);
	void assertSatisfied(JSExpr m);
	void newdiv(Integer cnt);
	JSExpr createAgent(CardName cardName);
	JSExpr createCard(CardName cardName);
	JSExpr createService(CardName name);
	JSExpr unmock(JSExpr mock);

	// support module commands
	JSExpr module(JSExpr runner, String jsName, String javaIF);
	
	// Send the block to disk
	JSExpr singleton();
	boolean isEmpty();
	void write(IndentWriter w);
	void generate(JVMCreationContext jvm);
}