package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSMockContract implements JSExpr {
	private final SolidName name;

	public JSMockContract(SolidName name) {
		this.name = name;
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("_cxt.mockContract(new ");
		w.print(name.jsName());
		w.print("(_cxt))");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr mc = md.callInterface(J.OBJECT, jvm.cxt(), "mockContract", md.castTo(jvm.cxt(), J.ERRORCOLLECTOR), md.classConst(name.javaName()));
		jvm.local(this, mc);
	}
}
