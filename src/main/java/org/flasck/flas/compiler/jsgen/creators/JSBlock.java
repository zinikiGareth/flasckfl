package org.flasck.flas.compiler.jsgen.creators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.compiler.jsgen.JSStyleIf;
import org.flasck.flas.compiler.jsgen.form.ExtractField;
import org.flasck.flas.compiler.jsgen.form.IVFWriter;
import org.flasck.flas.compiler.jsgen.form.IVForm;
import org.flasck.flas.compiler.jsgen.form.IsAExpr;
import org.flasck.flas.compiler.jsgen.form.IsConstExpr;
import org.flasck.flas.compiler.jsgen.form.IsTrueExpr;
import org.flasck.flas.compiler.jsgen.form.JSApplyHash;
import org.flasck.flas.compiler.jsgen.form.JSArray;
import org.flasck.flas.compiler.jsgen.form.JSArrayElt;
import org.flasck.flas.compiler.jsgen.form.JSAssertion;
import org.flasck.flas.compiler.jsgen.form.JSBind;
import org.flasck.flas.compiler.jsgen.form.JSCallMethod;
import org.flasck.flas.compiler.jsgen.form.JSCallStatic;
import org.flasck.flas.compiler.jsgen.form.JSCancelExpectation;
import org.flasck.flas.compiler.jsgen.form.JSClosure;
import org.flasck.flas.compiler.jsgen.form.JSContractByVar;
import org.flasck.flas.compiler.jsgen.form.JSCurry;
import org.flasck.flas.compiler.jsgen.form.JSCxtMethod;
import org.flasck.flas.compiler.jsgen.form.JSEffector;
import org.flasck.flas.compiler.jsgen.form.JSError;
import org.flasck.flas.compiler.jsgen.form.JSEval;
import org.flasck.flas.compiler.jsgen.form.JSExpectation;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSExtractFromBoundVar;
import org.flasck.flas.compiler.jsgen.form.JSField;
import org.flasck.flas.compiler.jsgen.form.JSFromCard;
import org.flasck.flas.compiler.jsgen.form.JSHead;
import org.flasck.flas.compiler.jsgen.form.JSIfExpr;
import org.flasck.flas.compiler.jsgen.form.JSIntroducedVar;
import org.flasck.flas.compiler.jsgen.form.JSKeepMessages;
import org.flasck.flas.compiler.jsgen.form.JSLiteral;
import org.flasck.flas.compiler.jsgen.form.JSLoadField;
import org.flasck.flas.compiler.jsgen.form.JSLocal;
import org.flasck.flas.compiler.jsgen.form.JSMakeAcor;
import org.flasck.flas.compiler.jsgen.form.JSMakeArray;
import org.flasck.flas.compiler.jsgen.form.JSMakeEventZone;
import org.flasck.flas.compiler.jsgen.form.JSMakeHash;
import org.flasck.flas.compiler.jsgen.form.JSMakeSend;
import org.flasck.flas.compiler.jsgen.form.JSMakeTuple;
import org.flasck.flas.compiler.jsgen.form.JSMember;
import org.flasck.flas.compiler.jsgen.form.JSMockAgent;
import org.flasck.flas.compiler.jsgen.form.JSMockCard;
import org.flasck.flas.compiler.jsgen.form.JSMockContract;
import org.flasck.flas.compiler.jsgen.form.JSMockHandler;
import org.flasck.flas.compiler.jsgen.form.JSMockService;
import org.flasck.flas.compiler.jsgen.form.JSModuleStmt;
import org.flasck.flas.compiler.jsgen.form.JSNew;
import org.flasck.flas.compiler.jsgen.form.JSNewDiv;
import org.flasck.flas.compiler.jsgen.form.JSNewState;
import org.flasck.flas.compiler.jsgen.form.JSPushConstructor;
import org.flasck.flas.compiler.jsgen.form.JSPushFunction;
import org.flasck.flas.compiler.jsgen.form.JSRecordContract;
import org.flasck.flas.compiler.jsgen.form.JSRenderObject;
import org.flasck.flas.compiler.jsgen.form.JSRequireContract;
import org.flasck.flas.compiler.jsgen.form.JSReturn;
import org.flasck.flas.compiler.jsgen.form.JSSatisfaction;
import org.flasck.flas.compiler.jsgen.form.JSSetField;
import org.flasck.flas.compiler.jsgen.form.JSSplitRWM;
import org.flasck.flas.compiler.jsgen.form.JSStoreField;
import org.flasck.flas.compiler.jsgen.form.JSStoreMock;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.jsgen.form.JSThis;
import org.flasck.flas.compiler.jsgen.form.JSTupleMember;
import org.flasck.flas.compiler.jsgen.form.JSUnmock;
import org.flasck.flas.compiler.jsgen.form.JSUpdateContainer;
import org.flasck.flas.compiler.jsgen.form.JSUpdateContent;
import org.flasck.flas.compiler.jsgen.form.JSUpdatePunnet;
import org.flasck.flas.compiler.jsgen.form.JSUpdateStyle;
import org.flasck.flas.compiler.jsgen.form.JSUpdateTemplate;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.flasck.flas.compiler.jsgen.form.JSWillSplitRWM;
import org.flasck.flas.compiler.jsgen.form.JSXCurry;
import org.flasck.flas.hsi.ArgSlot;
import org.flasck.flas.hsi.Slot;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.TemplateField;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;
import org.flasck.flas.parser.ut.UnitDataDeclaration;
import org.flasck.flas.tc3.NamedType;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.CantHappenException;

public class JSBlock implements JSBlockCreator {
	final List<JSExpr> stmts = new ArrayList<>();
	private final JSMethod creating;
	private final Map<String, JSLocal> fns;
	private final Map<JSExpr, JSLocal> closures;

	protected JSBlock() {
		this.creating = (JSMethod)this;
		this.fns = new HashMap<>();
		this.closures = new HashMap<>();
	}
	
	protected JSBlock(JSMethod creating, Map<String, JSLocal> fnBindings, Map<JSExpr, JSLocal> in) {
		this.creating = creating;
		fns = new HashMap<>(fnBindings);
		closures = new HashMap<>(in);
	}
	
	public boolean isEmpty() {
		return stmts.isEmpty();
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
	public JSString string(String string) {
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
		return new JSVar(var);
	}
	
	@Override
	public JSExpr lambda(HandlerLambda hl) {
		if (hl.patt instanceof TypedPattern)
			return new JSLoadField(new JSThis(), ((TypedPattern)hl.patt).var.var);
		else if (hl.patt instanceof VarPattern)
			return new JSLoadField(new JSThis(), ((VarPattern)hl.patt).var);
		else
			throw new CantHappenException("patt is " + hl.patt);
	}
	
	@Override
	public JSExpr member(NameOfThing type, String var) {
		return new JSMember(type, var);
	}
	
	@Override
	public JSExpr tupleMember(FunctionName name) {
		JSLocal stmt = new JSLocal(creating, new JSPushFunction(name, name.jsName(), -1));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr pushFunction(String meth, FunctionName name, int argcount) {
		JSLocal already = hasFn(meth);
		if (already != null)
			return already;
		JSLocal stmt = new JSLocal(creating, new JSPushFunction(name, meth, argcount));
		stmts.add(stmt);
		definedFn(meth, stmt);
		return stmt;
	}

	@Override
	public JSExpr pushConstructor(NameOfThing name, String clz) {
		JSLocal stmt = new JSLocal(creating, new JSPushConstructor(name, clz));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr structConst(NameOfThing name) {
		JSLocal stmt = new JSLocal(creating, new JSEval(name, new ArrayList<>()));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr structArgs(NameOfThing name, JSExpr... args) {
		JSLocal stmt = new JSLocal(creating, new JSEval(name, Arrays.asList(args)));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr callStatic(NameOfThing meth, int nargs) {
		JSLocal stmt = new JSLocal(creating, new JSCallStatic(meth, nargs));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr callMethod(String returnType, JSExpr obj, String method, JSExpr... args) {
		JSCallMethod m = new JSCallMethod(returnType, obj, method, args);
		if ("void".equals(returnType)) {
			stmts.add(m);
			return null;
		} else {
			JSLocal stmt = new JSLocal(creating, m);
			stmts.add(stmt);
			return stmt;
		}
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
		JSClosure tmp = new JSClosure(wantObject, args);
		JSLocal clos = haveClosure(tmp);
		if (clos != null)
			return clos;
		JSLocal stmt = new JSLocal(creating, tmp);
		stmts.add(stmt);
		closures.put(tmp, stmt);
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
	public JSExpr makeHash(List<JSExpr> args) {
		JSLocal ma = new JSLocal(creating, new JSMakeHash(args));
		stmts.add(ma);
		return ma;
	}

	@Override
	public JSExpr applyHash(JSExpr basic, JSExpr hash) {
		JSLocal ma = new JSLocal(creating, new JSApplyHash(basic, hash));
		stmts.add(ma);
		return ma;
	}

	@Override
	public JSExpr makeEventZone(JSExpr type, JSExpr expr) {
		JSLocal ma = new JSLocal(creating, new JSMakeEventZone(type, expr));
		stmts.add(ma);
		return ma;
	}

	@Override
	public JSExpr arrayElt(JSExpr tc, int i) {
		return new JSArrayElt(tc, i);
	}

	@Override
	public JSExpr introduceVar(String var) {
		if (var != null) {
			JSExpr iv = new JSLocal(creating, new JSIntroducedVar(var));
			stmts.add(iv);
			return iv;
		} else
			return new JSIntroducedVar(null);
	}

	@Override
	public JSExpr fromIntroduction(JSExpr boundVar) {
		return new JSExtractFromBoundVar(boundVar);
	}

	@Override
	public JSExpr jsArray(List<JSExpr> arr) {
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
	public JSExpr makeSend(String sendMeth, JSExpr obj, int nargs, JSExpr handler, JSExpr handlerName) {
		JSLocal ma = new JSLocal(creating, new JSMakeSend(sendMeth, obj, nargs, handler, handlerName));
		stmts.add(ma);
		return ma;
	}

	@Override
	public JSExpr makeAcor(FunctionName acorMeth, JSExpr obj, int nargs) {
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
	public JSExpr module(JSExpr runner, String jsName, String javaIF) {
		JSModuleStmt mod = new JSModuleStmt(runner, jsName, javaIF);
		JSLocal ret = new JSLocal(creating, mod);
		stmts.add(ret);
		return ret;
	}

	@Override
	public void renderObject(JSExpr runner, JSExpr obj, SolidName on, int which, JSString templateName) {
		JSRenderObject stmt = new JSRenderObject(runner, obj, on, which, templateName);
		stmts.add(stmt);
	}

	@Override
	public void expect(JSExpr obj, String assertion, List<JSExpr> args, JSExpr handler) {
		JSExpectation stmt = new JSExpectation(obj, assertion, args, handler);
		stmts.add(stmt);
	}
	
	@Override
	public void expectCancel(JSExpr obj) {
		JSCancelExpectation stmt = new JSCancelExpectation(obj);
		stmts.add(stmt);
	}
	
	@Override
	public JSExpr storeMockObject(UnitDataDeclaration udd, JSExpr value) {
		JSStoreMock store = new JSStoreMock(value);
		JSLocal ret = new JSLocal(creating, store);
		store.nameAs(ret);
		stmts.add(ret);
		return ret;
	}

	@Override
	public void assertSatisfied(JSExpr var) {
		JSSatisfaction stmt = new JSSatisfaction(var);
		stmts.add(stmt);
	}

	@Override
	public void newdiv(Integer cnt) {
		JSNewDiv stmt = new JSNewDiv(cnt);
		stmts.add(stmt);
	}

	@Override
	public void returnVoid() {
		JSReturn stmt = new JSReturn(null);
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
	public void updateContent(String templateName, TemplateField field, int option, JSExpr source, String fromField, JSExpr expr) {
		stmts.add(new JSUpdateContent(templateName, field, option, source, fromField, expr));
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
	public void updatePunnet(TemplateField field, JSExpr expr, int ucidx) {
		stmts.add(new JSUpdatePunnet(field, expr, ucidx));
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
	public void bindVar(Slot slot, JSExpr slotName, String var) {
		if (slot instanceof ArgSlot) {
			ArgSlot as = (ArgSlot) slot;
			if (as.isContainer())
				return;
		}
		stmts.add(new JSBind(slotName, var));
	}

	@Override
	public void head(JSExpr var) {
		stmts.add(new JSHead(var));
	}
	
	@Override
	public void splitRWM(JSExpr ocmsgs, JSExpr var) {
		stmts.add(new JSSplitRWM(ocmsgs, var));
	}
	
	@Override
	public void willSplitRWM(JSExpr r, JSExpr ocmsgs) {
		stmts.add(new JSWillSplitRWM(r, ocmsgs));
	}

	@Override
	public void keepMessages(JSExpr ocmsgs, JSExpr msgs) {
		stmts.add(new JSKeepMessages(ocmsgs, msgs));
	}

	@Override
	public void field(JSVar asVar, JSExpr fromVar, String field) {
		stmts.add(new ExtractField(asVar, fromVar, field));
	}

	@Override
	public JSIfCreator ifCtor(JSExpr expr, NameOfThing ctor) {
		JSIfExpr ret = new JSIfExpr(new IsAExpr(expr, ctor), new JSBlock(this.creating, fns, closures), new JSBlock(this.creating, fns, closures));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSIfCreator ifConst(JSExpr var, int cnst) {
		JSIfExpr ret = new JSIfExpr(new IsConstExpr(var, cnst), new JSBlock(this.creating, fns, closures), new JSBlock(this.creating, fns, closures));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSIfCreator ifConst(JSExpr var, String cnst) {
		JSIfExpr ret = new JSIfExpr(new IsConstExpr(var, cnst), new JSBlock(this.creating, fns, closures), new JSBlock(this.creating, fns, closures));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSIfCreator ifTrue(JSExpr ge) {
		JSIfExpr ret = new JSIfExpr(new IsTrueExpr(ge), new JSBlock(this.creating, fns, closures), new JSBlock(this.creating, fns, closures));
		stmts.add(ret);
		return ret;
	}

	@Override
	public void errorNoDefaultGuard() {
		stmts.add(new JSError(creating.getName() + ": no default guard"));
	}

	@Override
	public void errorNoCase() {
		stmts.add(new JSError(creating.getName() + ": no matching case"));
	}

	@Override
	public void error(JSExpr msg) {
		stmts.add(new JSError(msg));
	}

	@Override
	public JSLocal checkType(NamedType type, JSExpr res) {
		JSLocal ret = new JSLocal(creating, new IsAExpr(res, type.name()));
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
		JSMockCard card = new JSMockCard(name);
		JSLocal ret = new JSLocal(this.creating, card);
		card.nameAs(ret);
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSExpr createService(CardName name) {
		JSLocal ret = new JSLocal(this.creating, new JSMockService(name));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSExpr unmock(JSExpr mock) {
		return new JSUnmock(mock);
	}

	@Override
	public JSExpr fieldObject(String field, NameOfThing clz) {
		JSSetField ret = new JSSetField(false, field, new JSNew(clz));
		stmts.add(ret);
		return ret;
	}

	@Override
	public void stateField(boolean jsOnly) {
		stmts.add(new JSNewState(jsOnly));
	}

	@Override
	public void storeField(boolean jsOnly, JSExpr inObj, String field, JSExpr value) {
		stmts.add(new JSStoreField(jsOnly, inObj, field, value));
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
	public void setField(boolean jsOnly, String field, JSExpr expr) {
		stmts.add(new JSSetField(jsOnly, field, expr));
	}

	@Override
	public void setField(JSExpr on, String field, JSExpr expr) {
		stmts.add(new JSSetField(on, field, expr));
	}

	@Override
	public JSExpr field(String f) {
		JSField fld = new JSField(f);
//		stmts.add(fld);
		return fld;
	}

	@Override
	public JSExpr fromCard(NameOfThing name) {
		return new JSFromCard(name);
	}

	@Override
	public void recordContract(NameOfThing ctr, CSName impl) {
		stmts.add(new JSRecordContract(ctr, impl));
	}

	@Override
	public void requireContract(String var, NameOfThing impl) {
		stmts.add(new JSRequireContract(var, impl));
	}

	public JSLocal hasFn(String meth) {
		return fns.get(meth);
	}

	public void definedFn(String meth, JSLocal var) {
		fns.put(meth, var);
	}

	private JSLocal haveClosure(JSEffector tmp) {
		for (Entry<JSExpr, JSLocal> e : closures.entrySet()) {
			if (tmp.hasSameEffectAs(e.getKey()))
				return e.getValue();
		}
		return null;
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

	public void generate(JVMCreationContext jvm) {
		List<IExpr> blk = new ArrayList<>();
		for (JSExpr stmt : stmts) {
			stmt.generate(jvm);
			IExpr arg = jvm.stmt(stmt);
			if (arg != null)
				blk.add(arg);
		}
		jvm.block(this, blk);
	}

	public void asivm(IVFWriter iw) {
		for (JSExpr s : stmts) {
			if (s instanceof IVForm)
				((IVForm)s).asivm(iw.indent());
			else
				iw.indent().println(s.toString());
		}
	}
}
