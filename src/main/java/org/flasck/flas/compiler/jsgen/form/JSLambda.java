package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.flasck.flas.parsedForm.HandlerLambda;
import org.flasck.flas.parsedForm.TypedPattern;
import org.zinutils.bytecode.mock.IndentWriter;

public class JSLambda implements JSExpr {
	private final HandlerLambda lambda;

	public JSLambda(HandlerLambda lambda) {
		this.lambda = lambda;
	}

	@Override
	public String asVar() {
		return "this.state.get('" + ((TypedPattern)lambda.patt).var.var + "')";
	}

	@Override
	public void write(IndentWriter w, JVMCreationContext jvm) {
	}

}
