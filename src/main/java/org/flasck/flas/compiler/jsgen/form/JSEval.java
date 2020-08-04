package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

// I think this and JSCreateObject are basically the same
public class JSEval implements JSExpr {
	private final String clz;
	private final List<JSExpr> args;

	public JSEval(NameOfThing name) {
		this.clz = name.jsName();
		this.args = new ArrayList<>();
	}

	public JSEval(String name, List<JSExpr> args) {
		this.clz = name;
		this.args = args;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.print(clz);
		w.print(".eval(_cxt");
		for (JSExpr e : args) {
			w.print(", ");
			w.print(e.asVar());
		}
		w.print(")");
	}
}
