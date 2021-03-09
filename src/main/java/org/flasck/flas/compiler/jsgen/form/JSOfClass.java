package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.tc3.NamedType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSOfClass implements JSExpr {
	private final NamedType type;

	public JSOfClass(NamedType defn) {
		this.type = defn;
	}

	@Override
	public String asVar() {
		String tn = type.name().jsName();
		return tn;
	}

	@Override
	public void write(IndentWriter w) {
		throw new NotImplementedException();
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		String tn = type.name().javaName();
		jvm.local(this, md.classConst(tn));
	}
}
