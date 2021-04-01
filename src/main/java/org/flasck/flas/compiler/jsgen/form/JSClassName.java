package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSClassName implements IVForm {
	private final NameOfThing name;

	public JSClassName(NameOfThing name) {
		this.name = name;
	}

	@Override
	public String asVar() {
		return name.jsName();
	}

	@Override
	public void write(IndentWriter w) {
		w.print(asVar());
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		jvm.local(this, md.classConst(name.javaName()));
	}

	@Override
	public void asivm(IVFWriter iw) {
		iw.print("mockCard[" + name.uniqueName() + "]");
	}
}
