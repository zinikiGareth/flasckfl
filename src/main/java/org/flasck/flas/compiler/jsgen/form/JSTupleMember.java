package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.parsedForm.TupleMember;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSTupleMember implements JSExpr {
	private final TupleMember tm;

	public JSTupleMember(TupleMember tm) {
		this.tm = tm;
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
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

}
