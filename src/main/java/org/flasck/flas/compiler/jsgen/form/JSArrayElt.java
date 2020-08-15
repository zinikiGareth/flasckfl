package org.flasck.flas.compiler.jsgen.form;

import java.util.List;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSArrayElt implements JSExpr {
	private final JSExpr tc;
	private final int i;

	public JSArrayElt(JSExpr tc, int i) {
		this.tc = tc;
		this.i = i;
	}

	@Override
	public String asVar() {
		return tc.asVar() + "[" + i + "]";
	}

	@Override
	public void write(IndentWriter w) {
		throw new NotImplementedException("I don't think so, but probably just asVar");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner md = jvm.method();
		IExpr ag = md.callInterface(J.OBJECT, jvm.argAs(tc, new JavaType(List.class.getName())), "get", md.intConst(i));
		jvm.local(this, ag);
	}

}
