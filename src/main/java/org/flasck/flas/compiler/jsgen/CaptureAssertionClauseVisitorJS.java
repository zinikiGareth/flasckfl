package org.flasck.flas.compiler.jsgen;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.compiler.jsgen.creators.JSBlockCreator;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.parsedForm.ut.UnitTestIdentical;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;

public class CaptureAssertionClauseVisitorJS extends LeafAdapter implements ResultAware {
	private final JSFunctionState state;
	private NestedVisitor sv;
	private JSBlockCreator block;
	private JSExpr runner;
	private JSExpr value;
	private String op;

	public CaptureAssertionClauseVisitorJS(JSFunctionState state, NestedVisitor sv, JSBlockCreator block, JSExpr runner, String op) {
		this.state = state;
		this.sv = sv;
		this.block = block;
		this.runner = runner;
		this.op = op;
		sv.push(this);
	}

	@Override
	public void visitAssertExpr(boolean isValue, Expr e) {
		new ExprGeneratorJS(state, sv, block, false);
	}

	@Override
	public void result(Object r) {
		if (value == null)
			value = (JSExpr) r;
		else {
			JSExpr lhs = value;
			JSExpr rhs = (JSExpr) r;
			block.assertable(runner, op, lhs, rhs);
		}
	}

	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		sv.result(null);
	}

	@Override
	public void postUnitTestIdentical(UnitTestIdentical a) {
		sv.result(null);
	}
}
