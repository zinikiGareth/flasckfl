package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.parsedForm.TupleMember;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSTupleMember implements JSExpr {
	private final TupleMember tm;

	public JSTupleMember(TupleMember tm) {
		this.tm = tm;
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.tupleMember(");
		// TODO: there should be a higher level of sharing than this, but I'm not sure how
		// I think it goes into the more general question of Lambda Lifting
		w.print(tm.ta.name().jsName()+"(_cxt)");
		w.print(",");
		w.print(Integer.toString(tm.which));
		w.print(")");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr mt = md.callInterface(J.OBJECT, jvm.cxt(), "tupleMember",
			md.callStatic(tm.ta.exprFnName().javaClassName(), J.OBJECT, "eval", jvm.cxt(), md.arrayOf(J.OBJECT)),
			md.intConst(tm.which));
		jvm.local(this, mt);
	}

}
