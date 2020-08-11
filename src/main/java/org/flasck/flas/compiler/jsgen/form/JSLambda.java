package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.TypedPattern;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSLambda implements JSExpr {
	private final String var;

	public JSLambda(HandlerLambda lambda) {
		this.var = ((TypedPattern)lambda.patt).var.var;
	}

	@Override
	public String asVar() {
		return "this.state.get('" + var + "')";
	}

	@Override
	public void write(IndentWriter w) {
	}

	@Override
	public void generate(JVMCreationContext jvm) {
		jvm.local(this, jvm.method().getField(var));
	}

}
