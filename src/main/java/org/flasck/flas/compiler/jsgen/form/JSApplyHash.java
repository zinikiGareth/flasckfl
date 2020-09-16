package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSApplyHash implements JSExpr {
	private final JSExpr basic;
	private final JSExpr hash;

	public JSApplyHash(JSExpr basic, JSExpr hash) {
		this.basic = basic;
		this.hash = hash;
	}
	
	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.applyhash(");
		w.print(basic.asVar());
		w.print(", ");
		w.print(hash.asVar());
		w.print(")");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner meth = jvm.method();
		IExpr applied = meth.callInterface(J.OBJECT, jvm.cxt(), "applyhash", jvm.arg(basic), jvm.arg(hash));
		jvm.local(this, applied);
	}

}
