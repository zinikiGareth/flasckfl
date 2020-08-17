package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSCurry implements JSExpr {
	private final boolean wantObject;
	private final int required;
	private final JSExpr[] args;

	public JSCurry(boolean wantObject, int required, JSExpr... args) {
		this.wantObject = wantObject;
		this.required = required;
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
		if (wantObject) {
			w.print("_cxt.ocurry(");
			w.print(Integer.toString(required-1));
		} else {
			w.print("_cxt.curry(");
			w.print(Integer.toString(required));
		}
		for (JSExpr e : args) {
			w.print(", ");
			w.print(e.asVar());
		}
		w.print(")");
	}
	
	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr fn = null;
		List<IExpr> stack = new ArrayList<>();
		for (JSExpr e : args) {
			if (fn == null)
				fn = jvm.arg(e);
			else
				stack.add(jvm.arg(e));
		}
		IExpr as = md.arrayOf(J.OBJECT, stack);
		IExpr call;
		if (wantObject)
			call = md.callInterface(J.FLCURRY, jvm.cxt(), "ocurry", md.intConst(required-1), md.as(fn, J.APPLICABLE), as);
		else
			call = md.callInterface(J.FLCURRY, jvm.cxt(), "curry", md.intConst(required), md.as(fn, J.APPLICABLE), as);
		jvm.local(this, call);
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}
}
