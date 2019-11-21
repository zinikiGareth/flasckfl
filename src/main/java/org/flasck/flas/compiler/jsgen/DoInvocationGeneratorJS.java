package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class DoInvocationGeneratorJS extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final JSExpr runner;

	public DoInvocationGeneratorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block, JSExpr runner) {
		this.sv = sv;
		this.block = block;
		this.runner = runner;
		sv.push(this);
		new ExprGeneratorJS(state, sv, block);
	}

	@Override
	public void result(Object r) {
		block.assertable(runner, "invoke", (JSExpr) r);
		sv.result(null);
	}

}
