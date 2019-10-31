package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class CaptureAssertionClauseVisitorJS extends LeafAdapter implements ResultAware {
	private NestedVisitor sv;
	private JSBlockCreator block;
	private JSExpr runner;
	private JSExpr value;

	public CaptureAssertionClauseVisitorJS(NestedVisitor sv, JSBlockCreator block, JSExpr runner) {
		this.sv = sv;
		this.block = block;
		this.runner = runner;
		sv.push(this);
	}

	@Override
	public void visitAssertExpr(boolean isValue, Expr e) {
		sv.push(new ExprGeneratorJS(sv, block));
	}


	@Override
	public void result(Object r) {
		if (value == null)
			value = (JSExpr) r;
		else {
			JSExpr lhs = value;
			JSExpr rhs = (JSExpr) r;
			block.assertable(runner, "assertSameValue", lhs, rhs);
			sv.result(null);
		}
	}

}
