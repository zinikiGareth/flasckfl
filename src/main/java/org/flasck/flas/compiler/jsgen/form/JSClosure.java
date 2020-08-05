package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSClosure implements JSExpr, JSEffector {
	private final boolean wantObject;
	private final JSExpr[] args;

	public JSClosure(boolean wantObject, JSExpr... args) {
		this.wantObject = wantObject;
		this.args = args;
	}

	@Override
	public void write(IndentWriter w) {
		if (wantObject)
			w.print("_cxt.oclosure(");
		else
			w.print("_cxt.closure(");
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
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr fn = null;
		IExpr[] grp = new IExpr[args.length-1];
		for (int i=0;i<args.length;i++) {
			if (i == 0)
				fn = jvm.arg(args[i]);
			else
				grp[i-1] = jvm.arg(args[i]);
		}
		IExpr as = md.arrayOf(J.OBJECT, grp);

		IExpr call;
		if (wantObject)
			call = md.callInterface(J.FLCLOSURE, jvm.cxt(), "oclosure", md.as(fn, J.APPLICABLE), as);
		else
			call = md.callInterface(J.FLCLOSURE, jvm.cxt(), "closure", md.as(fn, J.APPLICABLE), as);
		jvm.local(this, call);
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public boolean hasSameEffectAs(JSExpr other) {
		if (!(other instanceof JSClosure))
			return false;
		JSClosure o = (JSClosure) other;
		if (wantObject != o.wantObject)
			return false;
		if (args.length != o.args.length)
			return false;
		for (int i=0;i<args.length;i++)
			if (args[i] != o.args[i])
				return false;
		return true;
	}
}
