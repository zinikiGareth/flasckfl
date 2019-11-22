package org.flasck.flas.compiler.jsgen.creators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.compiler.jsgen.form.ExtractField;
import org.flasck.flas.compiler.jsgen.form.IsAExpr;
import org.flasck.flas.compiler.jsgen.form.IsConstExpr;
import org.flasck.flas.compiler.jsgen.form.IsTrueExpr;
import org.flasck.flas.compiler.jsgen.form.JSAssertion;
import org.flasck.flas.compiler.jsgen.form.JSBind;
import org.flasck.flas.compiler.jsgen.form.JSBoundVar;
import org.flasck.flas.compiler.jsgen.form.JSCallMethod;
import org.flasck.flas.compiler.jsgen.form.JSClosure;
import org.flasck.flas.compiler.jsgen.form.JSCurry;
import org.flasck.flas.compiler.jsgen.form.JSError;
import org.flasck.flas.compiler.jsgen.form.JSEval;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSHead;
import org.flasck.flas.compiler.jsgen.form.JSIfExpr;
import org.flasck.flas.compiler.jsgen.form.JSLiteral;
import org.flasck.flas.compiler.jsgen.form.JSLoadField;
import org.flasck.flas.compiler.jsgen.form.JSLocal;
import org.flasck.flas.compiler.jsgen.form.JSMakeAcor;
import org.flasck.flas.compiler.jsgen.form.JSMakeArray;
import org.flasck.flas.compiler.jsgen.form.JSMakeSend;
import org.flasck.flas.compiler.jsgen.form.JSMockContract;
import org.flasck.flas.compiler.jsgen.form.JSNew;
import org.flasck.flas.compiler.jsgen.form.JSPushConstructor;
import org.flasck.flas.compiler.jsgen.form.JSPushFunction;
import org.flasck.flas.compiler.jsgen.form.JSReturn;
import org.flasck.flas.compiler.jsgen.form.JSStoreField;
import org.flasck.flas.compiler.jsgen.form.JSString;
import org.flasck.flas.compiler.jsgen.form.JSSetField;
import org.flasck.flas.compiler.jsgen.form.JSXCurry;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSBlock implements JSBlockCreator {
	private final List<JSExpr> stmts = new ArrayList<>();
	private final JSMethod creating;

	protected JSBlock() {
		this.creating = (JSMethod)this;
	}
	
	protected JSBlock(JSMethod creating) {
		this.creating = creating;
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
	public JSExpr newOf(SolidName clz) {
		JSLocal ret = new JSLocal(creating, new JSNew(clz));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSExpr boundVar(String var) {
		return new JSBoundVar(var);
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
	public JSExpr closure(JSExpr... args) {
		JSLocal stmt = new JSLocal(creating, new JSClosure(args));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSLocal curry(int expArgs, JSExpr... args) {
		JSLocal stmt = new JSLocal(creating, new JSCurry(expArgs, args));
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr xcurry(int expArgs, List<XCArg> posargs) {
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
	public JSExpr makeSend(String sendMeth, JSExpr obj, int nargs) {
		JSLocal ma = new JSLocal(creating, new JSMakeSend(sendMeth, obj, nargs));
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
	public void returnObject(JSExpr jsExpr) {
		JSReturn stmt = new JSReturn(jsExpr);
		stmts.add(stmt);
	}

	@Override
	public void bindVar(String slot, String var) {
		stmts.add(new JSBind(slot, var));
	}

	@Override
	public void head(String var) {
		stmts.add(new JSHead(var));
	}
	
	@Override
	public void field(String asVar, String fromVar, String field) {
		stmts.add(new ExtractField(asVar, fromVar, field));
	}

	@Override
	public JSIfExpr ifCtor(String var, String ctor) {
		JSIfExpr ret = new JSIfExpr(new IsAExpr(var, ctor), new JSBlock(this.creating), new JSBlock(this.creating));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSIfExpr ifConst(String var, int cnst) {
		JSIfExpr ret = new JSIfExpr(new IsConstExpr(var, cnst), new JSBlock(this.creating), new JSBlock(this.creating));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSIfExpr ifConst(String var, String cnst) {
		JSIfExpr ret = new JSIfExpr(new IsConstExpr(var, cnst), new JSBlock(this.creating), new JSBlock(this.creating));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSIfExpr ifTrue(JSExpr ge) {
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
	public JSExpr mockContract(SolidName name) {
		JSLocal ret = new JSLocal(creating, new JSMockContract(name));
		stmts.add(ret);
		return ret;
	}

	@Override
	public JSExpr createObject(SolidName name) {
		JSLocal ret = new JSLocal(this.creating, new JSEval(name));
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
	public void storeField(JSExpr inObj, String field, JSExpr value) {
		stmts.add(new JSStoreField(inObj, field, value));
	}

	@Override
	public JSLoadField loadField(String field) {
		return new JSLoadField(field);
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

	public static JSBlock classMethod(JSClass jsClass) {
		return new JSBlock(null);
	}
}
