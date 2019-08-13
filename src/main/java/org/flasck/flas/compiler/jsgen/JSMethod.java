package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSMethod implements JSMethodCreator {
	private final String name;
	private final List<JSVar> args = new ArrayList<>();
	private final List<JSExpr> stmts = new ArrayList<>();

	public JSMethod(String name) {
		this.name = name;
	}
	
	// handling quotes for strings - would it be better to separate strings out?
	@Override
	public JSExpr literal(String text) {
		return new JSLiteral(text);
	}

	@Override
	public JSExpr string(String string) {
		return null;
	}

	@Override
	public JSExpr callStatic(String clz, String meth, JSExpr... args) {
		return null;
	}

	@Override
	public JSExpr argument(String name) {
		JSVar ret = new JSVar(name);
		args.add(ret);
		return ret;
	}

	@Override
	public JSExpr callMethod(JSExpr obj, String meth, JSExpr... args) {
		JSCall stmt = new JSCall(obj, meth, args);
		stmts.add(stmt);
		return stmt;
	}

	@Override
	public void write(IndentWriter w) {
		w.print(name);
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
