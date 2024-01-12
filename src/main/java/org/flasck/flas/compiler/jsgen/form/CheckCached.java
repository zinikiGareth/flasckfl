package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class CheckCached implements JSExpr {
	private final JSLocal var;

	public CheckCached(JSLocal stmt) {
		this.var = stmt;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("Not a var");
	}

	@Override
	public void write(IndentWriter w) {
		w.println("if (" + var.asVar() + ") return " + var.asVar() + ";");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr e = md.ifNotNull(jvm.argAsIs(var), md.returnObject(jvm.argAs(var, JavaType.object_)), null);
		jvm.local(this, e);
	}
}
