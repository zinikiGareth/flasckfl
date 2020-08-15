package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSCallStatic implements JSExpr {
	private final NameOfThing meth;
	private final int nargs;

	public JSCallStatic(NameOfThing meth, int nargs) {
		this.meth = meth;
		this.nargs = nargs;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.makeStatic(");
		w.print(new JSString(meth.container().jsName()).asVar());
		w.print(",");
		w.print(new JSString(meth.baseName()).asVar());
		w.print(")");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		String name = meth.container().javaName();
		if ("FLBuiltin".equals(name))
			name = J.FLEVAL;
		IExpr expr = md.makeNew(J.CALLSTATIC, md.classConst(name), md.stringConst(meth.baseName()), md.intConst(nargs));
		jvm.local(this, expr);
	}
}
