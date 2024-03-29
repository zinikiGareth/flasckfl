package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.ut.UnitTestExpectCancel;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class DoExpectationCancelGeneratorJS extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private JSExpr mock;

	public DoExpectationCancelGeneratorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block) {
		this.sv = sv;
		this.block = block;
		sv.push(this);
		new ExprGeneratorJS(state, sv, block, true);
	}

	@Override
	public void result(Object r) {
		this.mock = (JSExpr) r;
	}

	@Override
	public void leaveUnitTestExpectCancel(UnitTestExpectCancel ute) {
		block.expectCancel(this.mock);
		sv.result(null);
	}
}
