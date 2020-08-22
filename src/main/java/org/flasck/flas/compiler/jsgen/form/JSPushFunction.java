package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSPushFunction implements JSExpr {
	private final FunctionName name;
	private final String fn;
	private final int argCount;

	public JSPushFunction(FunctionName name, String fn, int argcount) {
		this.name = name;
		this.fn = fn;
		argCount = argcount;
	}

	@Override
	public void write(IndentWriter w) {
		w.print(fn);
	}
	
	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		NameOfThing clzName = name.containingCard();
		if (clzName == null) {
			String push = jvm.figureName(name);
			jvm.local(this, md.makeNew(J.CALLEVAL, md.classConst(push)));
		} else {
			jvm.local(this, md.makeNew(J.CALLMETHOD, md.classConst(name.javaClassName()), md.stringConst(name.javaMethodName()), md.intConst(argCount)));
		}
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}
}
