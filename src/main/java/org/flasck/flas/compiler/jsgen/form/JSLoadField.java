package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.JavaType;
import org.zinutils.bytecode.NewMethodDefiner;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSLoadField implements JSExpr {
	private final JSExpr container;
	private final String field;

	public JSLoadField(JSExpr container, String field) {
		if (container == null) {
			throw new NotImplementedException();
		}
		this.container = container;
		this.field = field;
	}

	@Override
	public String asVar() {
		return container.asVar() + ".state.get('" + field + "')";
	}

	@Override
	public void write(IndentWriter w) {
		throw new RuntimeException("You shouldn't write this");
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		NewMethodDefiner meth = jvm.method();
		if (!jvm.hasLocal(container))
			container.generate(jvm);
		IExpr ret = meth.callInterface(J.OBJECT, jvm.argAs(container, new JavaType(J.FIELDS_CONTAINER_WRAPPER)), "get", meth.stringConst(field));
		jvm.local(this, ret);
	}
}
