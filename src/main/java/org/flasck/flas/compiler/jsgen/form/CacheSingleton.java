package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class CacheSingleton implements JSExpr {
	private final String fnName;
	private final JSExpr r;

	public CacheSingleton(String fnName, JSExpr r) {
		this.fnName = fnName;
		this.r = r;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("Not a var");
	}

	@Override
	public void write(IndentWriter w) {
		w.println("_cxt.cacheSingleton('" + fnName + "', " + r.asVar() + ");");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr e = md.callInterface("void", jvm.cxt(), "cacheSingleton", md.stringConst(fnName), jvm.argAs(r, JavaType.object_));
		jvm.local(this, e);
	}
}
