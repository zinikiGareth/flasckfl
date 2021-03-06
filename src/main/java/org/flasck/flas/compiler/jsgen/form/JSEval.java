package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

// I think this and JSCreateObject are basically the same
public class JSEval implements JSExpr {
	private final NameOfThing name;
	private final String clz;
	private final List<JSExpr> args;

	public JSEval(NameOfThing name) {
		this.name = name;
		this.clz = name.jsName();
		this.args = new ArrayList<>();
	}

	public JSEval(NameOfThing name, List<JSExpr> list) {
		this.name = name;
		this.clz = name.jsName();
		this.args = list;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print(clz);
		w.print(".eval(_cxt");
		for (JSExpr e : args) {
			w.print(", ");
			w.print(e.asVar());
		}
		w.print(")");
	}
	
	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr[] grp = new IExpr[args.size()];
		for (int i=0;i<args.size();i++) {
			JSExpr ai = args.get(i);
			grp[i] = jvm.arg(ai);
		}
		IExpr val = md.callStatic(jvm.figureName(name), J.OBJECT, "eval", jvm.cxt(), md.arrayOf(J.OBJECT, grp));
		jvm.local(this, val);
	}
}
