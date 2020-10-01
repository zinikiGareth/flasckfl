package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.CastExpr;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class CastExprGeneratorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private final NestedVisitor sv;
	private final JSBlockCreator block;
	private final boolean isExpectation;
	private JSExpr res;

	public CastExprGeneratorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block, boolean isExpectation) {
		this.state = state;
		this.sv = sv;
		this.block = block;
		this.isExpectation = isExpectation;
		sv.push(this);
	}

	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGeneratorJS(state, sv, block, isExpectation);
	}
	
	@Override
	public void result(Object r) {
		res = (JSExpr) r;
	}
	
	@Override
	public void leaveCastExpr(CastExpr expr) {
		sv.result(res);
	}
}
