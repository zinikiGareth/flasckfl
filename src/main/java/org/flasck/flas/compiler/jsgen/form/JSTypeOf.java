package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.tc3.NamedType;
import org.flasck.jvm.J;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSTypeOf implements JSExpr {
	private final NamedType type;

	public JSTypeOf(NamedType defn) {
		this.type = defn;
	}

	@Override
	public String asVar() {
		String tn = type.name().jsName();
		if ("Number".equals(tn))
			tn = "'number'";
		return "new TypeOf(" + tn + ")";
	}

	@Override
	public void write(IndentWriter w) {
		throw new NotImplementedException();
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		String tn = type.name().javaName();
		if ("org.flasck.jvm.builtin.Number".equals(tn))
			tn = Double.class.getName();
		jvm.local(this, jvm.method().makeNew(J.TYPEOF, jvm.method().classConst(tn)));
	}

}
