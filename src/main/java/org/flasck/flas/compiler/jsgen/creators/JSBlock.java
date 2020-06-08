package org.flasck.flas.compiler.jsgen.creators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.compiler.jsgen.JSStyleIf;
import org.flasck.flas.compiler.jsgen.form.ExtractField;
import org.flasck.flas.compiler.jsgen.form.IsAExpr;
import org.flasck.flas.compiler.jsgen.form.IsConstExpr;
import org.flasck.flas.compiler.jsgen.form.IsTrueExpr;
import org.flasck.flas.compiler.jsgen.form.JSArray;
import org.flasck.flas.compiler.jsgen.form.JSArrayElt;
import org.flasck.flas.compiler.jsgen.form.JSAssertion;
import org.flasck.flas.compiler.jsgen.form.JSBind;
import org.flasck.flas.compiler.jsgen.form.JSBoundVar;
import org.flasck.flas.compiler.jsgen.form.JSCallMethod;
import org.flasck.flas.compiler.jsgen.form.JSCheckType;
import org.flasck.flas.compiler.jsgen.form.JSClosure;
import org.flasck.flas.compiler.jsgen.form.JSContractByVar;
import org.flasck.flas.compiler.jsgen.form.JSCurry;
import org.flasck.flas.compiler.jsgen.form.JSCxtMethod;
import org.flasck.flas.compiler.jsgen.form.JSError;
import org.flasck.flas.compiler.jsgen.form.JSEval;
import org.flasck.flas.compiler.jsgen.form.JSExpectation;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSExtractFromBoundVar;
import org.flasck.flas.compiler.jsgen.form.JSFromCard;
import org.flasck.flas.compiler.jsgen.form.JSHead;
import org.flasck.flas.compiler.jsgen.form.JSIfExpr;
import org.flasck.flas.compiler.jsgen.form.JSIntroducedVar;
import org.flasck.flas.compiler.jsgen.form.JSKeepMessages;
import org.flasck.flas.compiler.jsgen.form.JSLambda;
import org.flasck.flas.compiler.jsgen.form.JSLiteral;
import org.flasck.flas.compiler.jsgen.form.JSLoadField;
import org.flasck.flas.compiler.jsgen.form.JSLocal;
import org.flasck.flas.compiler.jsgen.form.JSMakeAcor;
import org.flasck.flas.compiler.jsgen.form.JSMakeArray;
import org.flasck.flas.compiler.jsgen.form.JSMakeSend;
import org.flasck.flas.compiler.jsgen.form.JSMakeTuple;
import org.flasck.flas.compiler.jsgen.form.JSMember;
import org.flasck.flas.compiler.jsgen.form.JSMockAgent;
import org.flasck.flas.compiler.jsgen.form.JSMockCard;
import org.flasck.flas.compiler.jsgen.form.JSMockContract;
import org.flasck.flas.compiler.jsgen.form.JSMockHandler;
import org.flasck.flas.compiler.jsgen.form.JSNew;
import org.flasck.flas.compiler.jsgen.form.JSNewDiv;
import org.flasck.flas.compiler.jsgen.form.JSNewState;
import org.flasck.flas.compiler.jsgen.form.JSPushConstructor;
import org.flasck.flas.compiler.jsgen.form.JSPushFunction;
import org.flasck.flas.compiler.jsgen.form.JSRecordContract;
import org.flasck.flas.compiler.jsgen.form.JSRequireContract;
import org.flasck.flas.compiler.jsgen.form.JSReturn;
import org.flasck.flas.compiler.jsgen.form.JSSatisfaction;
import org.flasck.flas.compiler.jsgen.form.JSSetField;
import org.flasck.flas.compiler.jsgen.form.JSSplitRWM;
import org.flasck.flas.compiler.jsgen.form.JSStoreField;
import org.flasck.flas.compiler.jsgen.form.JSStoreMock;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.jsgen.form.JSTupleMember;
import org.flasck.flas.compiler.jsgen.form.JSUpdateContainer;
import org.flasck.flas.compiler.jsgen.form.JSUpdateContent;
import org.flasck.flas.compiler.jsgen.form.JSUpdateStyle;
import org.flasck.flas.compiler.jsgen.form.JSUpdateTemplate;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.compiler.jsgen.form.JSXCurry;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.tc3.NamedType;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSBlock implements JSBlockCreator {
	final List<JSExpr> stmts = new ArrayList<>();
	private final JSMethod creating;

	protected JSBlock() {
		this.creating = (JSMethod)this;
	}
	
	protected JSBlock(JSMethod creating) {
		this.creating = creating;
	}
	
	public JSExpr singleton() {
		if (stmts.size() == 1)
			return stmts.get(0);
		return null;
	}
	
	@Override
	public JSExpr literal(String text) {
		return new JSLiteral(text);
	}

	@Override
	public JSExpr string(String string) {
		return new JSString(string);
	}

	@Override
	public JSExpr newOf(NameOfThing clz) {
		JSLocal ret = new JSLocal(creating, new JSNew(clz));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSExpr newOf(NameOfThing clz, List<JSExpr> args) {
		JSLocal ret = new JSLocal(creating, new JSNew(clz, args));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSVar arg(int pos) {
		return creating.args.get(pos);
	}

	@Override
	public JSExpr boundVar(String var) {
		return new JSBoundVar(var);
	}
	
	@Override
	public JSExpr lambda(HandlerLambda hl) {
		return new JSLambda(hl);
	}
	
	@Override
	public JSExpr member(String var) {
		return new JSMember(var);
	}
	
	@Override
	public JSExpr tupleMember(FunctionName name) {
		JSLocal stmt = new JSLocal(creating, new JSPushFunction(name.jsName()));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr pushFunction(String meth) {
		JSLocal stmt = new JSLocal(creating, new JSPushFunction(meth));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr pushConstructor(String clz) {
		JSLocal stmt = new JSLocal(creating, new JSPushConstructor(clz));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr structConst(String name) {
		JSLocal stmt = new JSLocal(creating, new JSEval(name, new ArrayList<>()));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr structArgs(String name, JSExpr... args) {
		JSLocal stmt = new JSLocal(creating, new JSEval(name, Arrays.asList(args)));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr callMethod(JSExpr obj, String meth, JSExpr... args) {
		JSLocal stmt = new JSLocal(creating, new JSCallMethod(obj, meth, args));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr callStatic(String clz, String meth) {
		JSLocal stmt = new JSLocal(creating, new JSCxtMethod("makeStatic", string(clz), string(meth)));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr cxtMethod(String meth, JSExpr... args) {
		JSLocal stmt = new JSLocal(creating, new JSCxtMethod(meth, args));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr defineTupleMember(TupleMember e) {
		JSLocal stmt = new JSLocal(creating, new JSTupleMember(e));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr closure(boolean wantObject, JSExpr... args) {
		JSLocal stmt = new JSLocal(creating, new JSClosure(wantObject, args));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSLocal curry(boolean wantObject, int expArgs, JSExpr... args) {
		JSLocal stmt = new JSLocal(creating, new JSCurry(wantObject, expArgs, args));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr xcurry(boolean wantObject, int expArgs, List<XCArg> posargs) {
		JSLocal stmt = new JSLocal(creating, new JSXCurry(expArgs, posargs));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr makeArray(JSExpr... args) {
		JSLocal ma = new JSLocal(creating, new JSMakeArray(args));
		stmts.add(ma);
		return ma;
	}

	@Override
	public JSExpr makeArray(List<JSExpr> args) {
		JSLocal ma = new JSLocal(creating, new JSMakeArray(args));
		stmts.add(ma);
		return ma;
	}

	@Override
	public JSExpr arrayElt(JSExpr tc, int i) {
		return new JSArrayElt(tc, i);
	}

	@Override
	public JSExpr introduceVar(String var) {
		JSExpr iv = new JSIntroducedVar();
		if (var != null) {
			iv = new JSLocal(creating, iv);
			stmts.add(iv);
		}
		return iv;
	}

	@Override
	public JSExpr fromIntroduction(JSExpr boundVar) {
		return new JSExtractFromBoundVar(boundVar);
	}

	@Override
	public JSExpr jsArray(Iterable<JSExpr> arr) {
		JSLocal ma = new JSLocal(creating, new JSArray(arr));
		stmts.add(ma);
		return ma;
	}

	@Override
	public JSExpr makeTuple(JSExpr... args) {
		JSLocal ma = new JSLocal(creating, new JSMakeTuple(args));
		stmts.add(ma);
		return ma;
	}

	@Override
	public JSExpr makeSend(String sendMeth, JSExpr obj, int nargs, JSExpr handler) {
		JSLocal ma = new JSLocal(creating, new JSMakeSend(sendMeth, obj, nargs, handler));
		stmts.add(ma);
		return ma;
	}

	@Override
	public JSExpr makeAcor(String acorMeth, JSExpr obj, int nargs) {
		JSLocal ma = new JSLocal(creating, new JSMakeAcor(acorMeth, obj, nargs));
		stmts.add(ma);
		return ma;
	}

	@Override
	public void assertable(JSExpr obj, String assertion, JSExpr... args) {
		JSAssertion stmt = new JSAssertion(obj, assertion, args);
		stmts.add(stmt);
	}

	@Override
	public void expect(JSExpr obj, String assertion, List<JSExpr> args, JSExpr handler) {
		JSExpectation stmt = new JSExpectation(obj, assertion, args, handler);
		stmts.add(stmt);
	}
	
	@Override
	public JSExpr storeMockObject(UnitDataDeclaration udd, JSExpr value) {
		JSLocal ret = new JSLocal(creating, new JSStoreMock(value));
		stmts.add(ret);
		return ret;
	}

	@Override
	public void assertSatisfied(String var) {
		JSSatisfaction stmt = new JSSatisfaction(var);
		stmts.add(stmt);
	}

	@Override
	public void newdiv(Integer cnt) {
		JSNewDiv stmt = new JSNewDiv(cnt);
		stmts.add(stmt);
	}

	@Override
	public void returnObject(JSExpr jsExpr) {
		JSReturn stmt = new JSReturn(jsExpr);
		stmts.add(stmt);
	}

	@Override
	public void returnCompare(JSExpr lhs, JSExpr rhs) {
		JSReturn stmt = new JSReturn(new JSCompare(lhs, rhs));
		stmts.add(stmt);
	}

	@Override
	public void updateContent(String templateName, TemplateField field, int option, JSExpr source, JSExpr expr) {
		stmts.add(new JSUpdateContent(templateName, field, option, source, expr));
	}

	@Override
	public void updateStyle(String templateName, TemplateField field, int option, JSExpr source, JSExpr constant, List<JSStyleIf> styles) {
		stmts.add(new JSUpdateStyle(templateName, field, option, source, constant, styles));
	}
	
	@Override
	public void updateContainer(TemplateField field, JSExpr expr, int ucidx) {
		stmts.add(new JSUpdateContainer(field, expr, ucidx));
	}

	@Override
	public void updateTemplate(TemplateField field, int posn, boolean isOtherObject, String templateName, JSExpr expr, JSExpr tc) {
		stmts.add(new JSUpdateTemplate(field, posn, isOtherObject, templateName, expr, tc));
	}

	@Override
	public void addItem(int posn, String templateName, JSExpr expr, JSExpr tc) {
		stmts.add(new JSAddItem(posn, templateName, expr, tc));
	}

	@Override
	public void bindVar(Slot slot, String slotName, String var) {
		if (slot instanceof ArgSlot) {
			ArgSlot as = (ArgSlot) slot;
			if (as.isContainer())
				return;
		}
		stmts.add(new JSBind(slotName, var));
	}

	@Override
	public void head(String var) {
		stmts.add(new JSHead(var));
	}
	
	@Override
	public void splitRWM(JSExpr ocmsgs, String var) {
		stmts.add(new JSSplitRWM(ocmsgs, var));
	}
	
	@Override
	public void keepMessages(JSExpr ocmsgs, JSExpr msgs) {
		stmts.add(new JSKeepMessages(ocmsgs, msgs));
	}

	@Override
	public void field(String asVar, String fromVar, String field) {
		stmts.add(new ExtractField(asVar, fromVar, field));
	}

	@Override
	public JSIfCreator ifCtor(String var, String ctor) {
		JSIfExpr ret = new JSIfExpr(new IsAExpr(var, ctor), new JSBlock(this.creating), new JSBlock(this.creating));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSIfCreator ifCtor(JSExpr expr, NameOfThing ctor) {
		JSIfExpr ret = new JSIfExpr(new IsAExpr(expr, ctor), new JSBlock(this.creating), new JSBlock(this.creating));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSIfCreator ifConst(String var, int cnst) {
		JSIfExpr ret = new JSIfExpr(new IsConstExpr(var, cnst), new JSBlock(this.creating), new JSBlock(this.creating));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSIfCreator ifConst(String var, String cnst) {
		JSIfExpr ret = new JSIfExpr(new IsConstExpr(var, cnst), new JSBlock(this.creating), new JSBlock(this.creating));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSIfCreator ifTrue(JSExpr ge) {
		JSIfExpr ret = new JSIfExpr(new IsTrueExpr(ge), new JSBlock(this.creating), new JSBlock(this.creating));
		stmts.add(ret);
		return ret;
	}

	@Override
	public void errorNoDefaultGuard() {
		stmts.add(new JSError("no default guard"));
	}

	@Override
	public void errorNoCase() {
		stmts.add(new JSError("no matching case"));
	}

	@Override
	public void error(JSExpr msg) {
		stmts.add(new JSError(msg));
	}

	@Override
	public JSLocal checkType(NamedType type, JSExpr res) {
		JSLocal ret = new JSLocal(creating, new JSCheckType(type, res));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSExpr mockContract(SolidName name) {
		JSLocal ret = new JSLocal(creating, new JSMockContract(name));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSExpr mockHandler(SolidName name) {
		JSLocal ret = new JSLocal(creating, new JSMockHandler(name));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSExpr createObject(NameOfThing name) {
		return createObject(name, new ArrayList<>());
	}

	@Override
	public JSExpr createObject(NameOfThing name, List<JSExpr> args) {
		return createObject(name.jsName(), args);
	}

	@Override
	public JSExpr createObject(String name, List<JSExpr> args) {
		JSLocal ret = new JSLocal(this.creating, new JSEval(name, args));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSExpr createAgent(CardName name) {
		JSLocal ret = new JSLocal(this.creating, new JSMockAgent(name));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSExpr createCard(CardName name) {
		JSLocal ret = new JSLocal(this.creating, new JSMockCard(name));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSExpr fieldObject(String field, String clz) {
		JSSetField ret = new JSSetField(field, new JSNew(clz));
		stmts.add(ret);
		return ret;
	}

	@Override
	public void stateField() {
		JSSetField state = new JSSetField("state", new JSNewState());
		stmts.add(state);
	}

	@Override
	public void storeField(JSExpr inObj, String field, JSExpr value) {
		stmts.add(new JSStoreField(inObj, field, value));
	}

	@Override
	public JSLoadField loadField(JSExpr container, String field) {
		return new JSLoadField(container, field);
	}

	@Override
	public JSExpr contractByVar(JSExpr container, String name) {
		return new JSContractByVar(container, name);
	}

	@Override
	public void setField(String field, JSExpr expr) {
		stmts.add(new JSSetField(field, expr));
	}

	@Override
	public void setField(JSExpr on, String field, JSExpr expr) {
		stmts.add(new JSSetField(on, field, expr));
	}

	@Override
	public JSExpr fromCard() {
		return new JSFromCard();
	}

	@Override
	public void recordContract(String ctr, String impl) {
		stmts.add(new JSRecordContract(ctr, impl));
	}

	@Override
	public void requireContract(String var, String impl) {
		stmts.add(new JSRequireContract(var, impl));
	}

	@Override
	public void write(IndentWriter w) {
		w.println("{");
		IndentWriter iw = w.indent();
		for (JSExpr stmt : stmts) {
			stmt.write(iw);
		}
		w.print("}");
	}
}
