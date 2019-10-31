package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

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
	public JSExpr boundVar(String var) {
		return new JSBoundVar(var);
	}
	
	@Override
	public JSExpr pushFunction(String meth) {
		JSPushFunction stmt = new JSPushFunction(creating, meth);
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr structConst(String name) {
		JSConstant stmt = new JSConstant(creating, name);
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr callFunction(String meth, JSExpr... args) {
		JSCallMethod stmt = new JSCallMethod(creating, null, meth, args);
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr callMethod(JSExpr obj, String meth, JSExpr... args) {
		JSCallMethod stmt = new JSCallMethod(creating, obj, meth, args);
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSClosure closure(JSExpr... args) {
		JSClosure stmt = new JSClosure(creating, args);
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSCurry curry(int expArgs, JSExpr... args) {
		JSCurry stmt = new JSCurry(creating, expArgs, args);
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr xcurry(int expArgs, List<XCArg> posargs) {
		JSXCurry stmt = new JSXCurry(creating, expArgs, posargs);
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr makeArray(JSExpr... args) {
		JSMakeArray ma = new JSMakeArray(creating, args);
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
	public void write(IndentWriter w) {
		w.println("{");
		IndentWriter iw = w.indent();
		for (JSExpr stmt : stmts) {
			stmt.write(iw);
		}
		w.print("}");
	}
}
