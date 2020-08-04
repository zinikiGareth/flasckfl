package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSPushFunction implements JSExpr {
	private final FunctionName name;
	private final String fn;

	public JSPushFunction(FunctionName name, String fn) {
		this.name = name;
		this.fn = fn;
	}

	@Override
	public void write(IndentWriter w) {
		w.print(fn);
	}
	
	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		String push = jvm.figureName(name);
		System.out.println("pushing fn name " + push);
		jvm.local(this, md.makeNew(J.CALLEVAL, md.classConst(push)));
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}
}
