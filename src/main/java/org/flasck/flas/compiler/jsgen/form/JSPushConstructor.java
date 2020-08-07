package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSPushConstructor implements JSExpr {
	private final NameOfThing name;
	private final String clz;

	public JSPushConstructor(NameOfThing name, String clz) {
		this.name = name;
		this.clz = clz;
	}

	@Override
	public void write(IndentWriter w) {
		w.print(clz);
		w.print(".eval");
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		jvm.local(this, md.makeNew(J.CALLEVAL, md.classConst(jvm.figureName(name))));
	}
}
