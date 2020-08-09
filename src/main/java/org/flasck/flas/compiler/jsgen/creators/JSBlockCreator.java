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
	JSExpr string(String string);
	JSExpr nameOf(JSExpr expr);
	JSExpr newOf(NameOfThing clz);
	JSExpr newOf(NameOfThing clz, List<JSExpr> args);
	JSExpr makeArray(JSExpr... args);
	JSExpr makeArray(List<JSExpr> args);
	JSExpr jsArray(List<JSExpr> arr);
	JSExpr arrayElt(JSExpr tc, int i);
	JSExpr makeTuple(JSExpr... args);
	JSExpr callMethod(JSExpr obj, String meth, JSExpr... args);
	JSExpr callStatic(String clz, String meth);
	JSExpr cxtMethod(String meth, JSExpr... args);
	JSExpr pushFunction(String meth, FunctionName name);
	JSExpr pushConstructor(NameOfThing name, String clz);
	JSExpr defineTupleMember(TupleMember e);
	void returnObject(JSExpr jsExpr);
	void returnCompare(JSExpr lhs, JSExpr rhs);

	// creating more complex things
	JSExpr structConst(NameOfThing name);
	JSExpr mockContract(SolidName name);
	JSExpr mockHandler(SolidName name);
	JSExpr createObject(NameOfThing name);
	JSExpr createObject(NameOfThing name, List<JSExpr> args);
	JSExpr createAgent(CardName cardName);
	JSExpr createCard(CardName cardName);
	JSExpr makeSend(String sendMeth, JSExpr obj, int nargs, JSExpr handler);
	JSExpr makeAcor(FunctionName acorMeth, JSExpr obj, int nargs);
	JSExpr introduceVar(String var);
	JSExpr fromIntroduction(JSExpr boundVar);
	void recordContract(NameOfThing ctr, CSName impl);
	void requireContract(String referAsVar, String jsName);
	
	// create an object of clz and store in a field
	JSExpr fieldObject(String field, String clz);
	void stateField();
	void setField(String field, JSExpr value);
	void setField(JSExpr obj, String field, JSExpr value);
	JSExpr fromCard();
	JSVar arg(int pos);

	// Stored values
	JSExpr boundVar(String var);
	JSExpr tupleMember(FunctionName name);
	JSExpr lambda(HandlerLambda defn);
	JSExpr member(String var);

	// HSIE logic statements
	void bindVar(Slot slot, JSExpr jsExpr, String var);
	void head(JSExpr currentVar, Slot slot);
	void splitRWM(JSExpr ocmsgs, JSExpr currentVar);
	void willSplitRWM(JSExpr r, JSExpr ocmsgs);
	void keepMessages(JSExpr ocmsgs, JSExpr r);
	void field(JSVar jv, JSExpr jsExpr, String field, Slot slot);
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
	void updateContent(String templateName, TemplateField field, int option, JSExpr source, JSExpr expr);
	void updateStyle(String templateName, TemplateField field, int option, JSExpr source, JSExpr constant, List<JSStyleIf> styles);
	void updateTemplate(TemplateField field, int posn, boolean isOtherObject, String templateName, JSExpr expr, JSExpr tc);
	void updateContainer(TemplateField field, JSExpr expr, int ucidx);
	void addItem(int position, String templateName, JSExpr expr, JSExpr makeArray);
	
	// unit testing
	void assertable(JSExpr runner, String assertion, JSExpr... args);
	void expect(JSExpr mock, String var, List<JSExpr> args, JSExpr handler);
	JSExpr storeMockObject(UnitDataDeclaration udd, JSExpr value);
	void assertSatisfied(JSExpr m);
	void newdiv(Integer cnt);

	// Send the block to disk
	JSExpr singleton();
	boolean isEmpty();
	void write(IndentWriter w);
	void generate(JVMCreationContext jvm);
}