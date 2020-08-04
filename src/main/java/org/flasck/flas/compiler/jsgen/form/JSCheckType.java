package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.tc3.NamedType;
import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSCheckType implements JSExpr {
	private final NamedType type;
	private final JSExpr res;

	public JSCheckType(NamedType type, JSExpr res) {
		this.type = type;
		this.res = res;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("not for use");
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
		w.print("_cxt.isA(");
		w.print(res.asVar());
		w.print(", '");
		w.print(type.name().uniqueName());
		w.print("')");
	}

}
