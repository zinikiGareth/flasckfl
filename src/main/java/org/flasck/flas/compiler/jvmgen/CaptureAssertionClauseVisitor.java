package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.ut.UnitTestAssert;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryVisitor;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;

public class CaptureAssertionClauseVisitor extends LeafAdapter implements RepositoryVisitor, ResultAware {
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
		new ExprGenerator(fs, sv, block, false);
	}

	@Override
	public void result(Object r) {
		if (value == null)
			value = (IExpr) r;
		else {
			IExpr lhs = meth.as(value, J.OBJECT);
			IExpr rhs = meth.as((IExpr) r, J.OBJECT);
			IExpr ret = meth.callInterface("void", runner, "assertSameValue", fs.fcx, lhs, rhs);
			block.add(ret);
			JVMGenerator.makeBlock(meth, block).flush();
		}
	}
	
	@Override
	public void postUnitTestAssert(UnitTestAssert a) {
		sv.result(null);
	}
}
