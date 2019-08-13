package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSMethod implements JSMethodCreator {
	private final String pkg;
	private final String name;
	private final List<JSVar> args = new ArrayList<>();
	private final List<JSExpr> stmts = new ArrayList<>();

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
	public JSExpr callStatic(String clz, String meth, JSExpr... args) {
		JSCallFunction stmt = new JSCallFunction(clz, meth, args);
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
		JSCallMethod stmt = new JSCallMethod(obj, meth, args);
		stmts.add(stmt);
		return stmt;
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
}
