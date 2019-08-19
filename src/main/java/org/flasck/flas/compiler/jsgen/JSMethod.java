package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSMethod implements JSMethodCreator {
	private final String pkg;
	private final String name;
	private final List<JSVar> args = new ArrayList<>();
	private final List<JSExpr> stmts = new ArrayList<>();
	private int nextVar = 1;

	public JSMethod(String pkg, String name) {
		this.pkg = pkg;
		this.name = name;
	}
	
	// handling quotes for strings - would it be better to separate strings out?
	@Override
	public JSExpr literal(String text) {
		return new JSLiteral(text);
	}

	@Override
	public JSExpr string(String string) {
		return new JSString(string);
	}

	@Override
	public JSExpr pushFunction(String meth) {
		JSPushFunction stmt = new JSPushFunction(this, meth);
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSExpr argument(String name) {
		JSVar ret = new JSVar(name);
		args.add(ret);
		return ret;
	}

	@Override
	public JSExpr callMethod(JSExpr obj, String meth, JSExpr... args) {
		JSCallMethod stmt = new JSCallMethod(this, obj, meth, args);
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSClosure closure(JSExpr... args) {
		JSClosure stmt = new JSClosure(this, args);
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public JSClosure curry(int expArgs, JSExpr... args) {
//		JSClosure stmt = new JSClosure(this, args);
//		stmts.add(stmt);
//		return stmt;
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
	public void write(IndentWriter w) {
		w.println("");
		w.print(pkg);
		w.print(".");
		w.print(name);
		w.print(" = function");
		w.print("(");
		boolean isFirst = true;
		for (JSVar v : args) {
			if (isFirst)
				isFirst = false;
			else
				w.print(", ");
			w.print(v.asVar());
		}
		w.println(") {");
		IndentWriter iw = w.indent();
		for (JSExpr stmt : stmts) {
			stmt.write(iw);
		}
		w.println("}");
	}

	public String obtainNextVar() {
		return "v" + nextVar ++;
	}
}
