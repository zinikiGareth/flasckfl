package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.flasck.flas.repository.Repository.Visitor;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class CaptureAssertionClauseVisitor extends LeafAdapter implements Visitor, ResultAware {
	private final NestedVisitor sv;
	private final FunctionState fs;
	private final MethodDefiner meth;
	private final IExpr runner;
	private IExpr value;
	private List<IExpr> block = new ArrayList<>();

	public CaptureAssertionClauseVisitor(StackVisitor sv, FunctionState fs, IExpr runner) {
		this.sv = sv;
		this.fs = fs;
		this.runner = runner;
		this.meth = fs.meth;
		if (sv != null)
			sv.push(this);
	}

	@Override
	public void visitAssertExpr(boolean isValue, Expr e) {
		new ExprGenerator(fs, sv, block);
	}

	@Override
	public void result(Object r) {
		if (value == null)
			value = (IExpr) r;
		else {
			IExpr lhs = meth.as(value, J.OBJECT);
			IExpr rhs = meth.as((IExpr) r, J.OBJECT);
			IExpr ret = meth.callVirtual("void", runner, "assertSameValue", lhs, rhs);
			block.add(ret);
			sv.result(JVMGenerator.makeBlock(meth, block));
		}
	}
}
