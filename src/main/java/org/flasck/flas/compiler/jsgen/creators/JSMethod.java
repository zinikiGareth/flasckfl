package org.flasck.flas.compiler.jsgen.creators;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMethod extends JSBlock implements JSMethodCreator {
	private final String pkg;
	private final boolean prototype;
	private final String name;
	private final List<JSVar> args = new ArrayList<>();
	private int nextVar = 1;

	public JSMethod(String pkg, boolean prototype, String name) {
		this.pkg = pkg;
		this.prototype = prototype;
		this.name = name;
	}
	
	public String getPackage() {
		return pkg;
	}
	
	public String getName() {
		return name;
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
		if (prototype)
			w.print("prototype.");
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
		w.println("");
		w.print(pkg);
		w.print(".");
		if (prototype)
			w.print("prototype.");
		w.print(name);
		w.print(".nfargs = function() { return ");
		w.print(Integer.toString(args.size() - 1)); // -1 for context
		w.println("; }");
	}

	public String obtainNextVar() {
		return "v" + nextVar ++;
	}
}
