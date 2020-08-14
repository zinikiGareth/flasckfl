package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMakeTuple implements JSExpr {
	private final JSExpr[] args;

	public JSMakeTuple(JSExpr... args) {
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.makeTuple(");
		boolean isFirst = true;
		for (JSExpr e : args) {
			if (isFirst)
				isFirst = false;
			else
				w.print(", ");
			w.print(e.asVar());
		}
		w.print(")");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		List<IExpr> arr = new ArrayList<IExpr>();
		for (JSExpr e : args) {
			arr.add(jvm.arg(e));
		}
		IExpr mt = md.callInterface(J.OBJECT, jvm.cxt(), "makeTuple", md.arrayOf(J.OBJECT, arr));
		jvm.local(this, mt);
	}

}
