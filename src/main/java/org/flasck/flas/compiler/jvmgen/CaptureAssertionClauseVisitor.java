package org.flasck.flas.compiler.jvmgen;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.jvm.J;
import org.flasck.flas.repository.Repository.Visitor;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class CaptureAssertionClauseVisitor extends LeafAdapter implements Visitor, ResultAware {
	private NestedVisitor sv;
	private MethodDefiner meth;
	private IExpr runner;
	private IExpr fcx;
	private IExpr value;

	public CaptureAssertionClauseVisitor(NestedVisitor sv, MethodDefiner meth, IExpr runner, IExpr fcx) {
		this.sv = sv;
		this.meth = meth;
		this.runner = runner;
		this.fcx = fcx;
		sv.push(this);
	}

	@Override
	public void visitAssertExpr(boolean isValue, Expr e) {
		sv.push(new ExprGenerator(new FunctionState(meth, fcx, null), sv));
	}

	@Override
	public void result(Object r) {
		if (value == null)
			value = (IExpr) r;
		else {
			IExpr lhs = meth.as(value, J.OBJECT);
			IExpr rhs = meth.as((IExpr) r, J.OBJECT);
			IExpr ret = meth.callVirtual("void", runner, "assertSameValue", lhs, rhs);
			sv.result(ret);
		}
	}
}
