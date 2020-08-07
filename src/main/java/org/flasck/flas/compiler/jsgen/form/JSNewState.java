package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.JavaInfo.Access;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSNewState implements JSExpr {

	public JSNewState() {
	}

	@Override
	public String asVar() {
		throw new RuntimeException("This should be wrapped in a JSLocal or JSThis");
	}

	@Override
	public void write(IndentWriter w) {
		w.println("this.state = _cxt.fields();");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		jvm.inherit(true, Access.PROTECTED, J.FIELDS_CONTAINER, "state");
		jvm.local(this, jvm.method().callSuper("void", J.JVM_FIELDS_CONTAINER_WRAPPER, "<init>", jvm.cxt()));
	}
}
