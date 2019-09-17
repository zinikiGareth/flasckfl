package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.zinutils.bytecode.mock.IndentWriter;

public class JSMethod extends JSBlock implements JSMethodCreator {
	private final String pkg;
	private final String name;
	private final List<JSVar> args = new ArrayList<>();
	private int nextVar = 1;

	public JSMethod(String pkg, String name) {
		this.pkg = pkg;
		this.name = name;
	}
	
	@Override
	public JSExpr argument(String name) {
		JSVar ret = new JSVar(name);
		args.add(ret);
		return ret;
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
		w.print(") ");
		super.write(w);
		w.println("");
	}

	public String obtainNextVar() {
		return "v" + nextVar ++;
	}
}
