package org.flasck.flas.compiler.jsgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class DoExpectationGeneratorJS extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final JSFunctionState state;
	private JSExpr mock;
	private List<JSExpr> args = new ArrayList<>();
	private boolean isHandler;
	private JSExpr handler;

	public DoExpectationGeneratorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block) {
		this.state = state;
		this.sv = sv;
		this.block = block;
		sv.push(this);
		new ExprGeneratorJS(state, sv, block, true);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGeneratorJS(state, sv, block, true);
	}

	@Override
	public void expectHandlerNext() {
		this.isHandler = true;
	}
	
	@Override
	public void result(Object r) {
		// first result is the mock
		if (mock == null)
			this.mock = (JSExpr) r;
		else if (isHandler)
			this.handler = (JSExpr)r;
		else
			this.args.add((JSExpr) r);
	}

	@Override
	public void leaveUnitTestExpect(UnitTestExpect ute) {
		block.expect(this.mock, ute.method.var, this.args, this.handler);
		sv.result(null);
	}
}
