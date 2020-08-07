package org.flasck.flas.compiler.jsgen.form;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.compiler.jsgen.JSGenerator.XCArg;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSXCurry implements JSExpr {
	private final boolean wantObject = false; // TODO: should this be a parameter?
	private final List<XCArg> args;
	private final int required;

	public JSXCurry(int required, List<XCArg> posargs) {
		this.required = required;
		this.args = posargs;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.xcurry(" + required);
		for (XCArg e : args) {
			w.print(", ");
			w.print(Integer.toString(e.arg));
			w.print(", ");
			w.print(e.expr.asVar());
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
		IExpr fn = null;
		List<IExpr> stack = new ArrayList<>();
		for (XCArg xc : args) {
			JSExpr e = xc.expr;
			if (!jvm.hasLocal(e))
				e.generate(jvm);
			if (fn == null)
				fn = jvm.arg(e);
			else {
				stack.add(md.box(md.intConst(xc.arg-1))); // TODO: this appears to be a difference in the JVM and JS runtimes
				stack.add(jvm.arg(e));
			}
		}
		IExpr xcs = md.arrayOf(J.OBJECT, stack);
		IExpr call;
		if (wantObject)
			call = md.callInterface(J.FLCURRY, jvm.cxt(), "oxcurry", md.intConst(required-1), md.as(fn, J.APPLICABLE), xcs);
		else
			call = md.callInterface(J.FLCURRY, jvm.cxt(), "xcurry", md.intConst(required), md.as(fn, J.APPLICABLE), xcs);
		jvm.local(this, call);
	}
}
