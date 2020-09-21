package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSMember implements JSExpr {
	private final NameOfThing type;
	private final String var;

	public JSMember(NameOfThing type, String var) {
		this.type = type;
		this.var = var;
	}

	@Override
	public String asVar() {
		return "this." + var;
	}

	@Override
	public void write(IndentWriter w) {
		throw new NotImplementedException();
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr f = md.getField(var);
		if (type != null)
			f = md.castTo(f, type.javaName());
		jvm.local(this, f);
	}

}
