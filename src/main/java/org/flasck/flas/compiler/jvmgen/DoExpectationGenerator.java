package org.flasck.flas.compiler.jvmgen;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.repository.StackVisitor;
import org.flasck.jvm.J;
import org.zinutils.bytecode.IExpr;

public class DoExpectationGenerator extends LeafAdapter implements ResultAware {
	private final StackVisitor sv;
	private final FunctionState fs;
	private IExpr mock;
	private List<IExpr> args = new ArrayList<>();
	private List<IExpr> block;

	public DoExpectationGenerator(StackVisitor sv, FunctionState fs, IExpr runner, List<IExpr> block) {
		this.sv = sv;

		this.fs = fs;
		this.block = block;
		sv.push(this);
		new ExprGenerator(fs, sv, block);
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		new ExprGenerator(fs, sv, block);
	}

	@Override
	public void result(Object r) {
		// first result is the mock
		if (mock == null)
			this.mock = fs.meth.castTo((IExpr) r, J.EXPECTING);
		else
			this.args.add((IExpr) r);
	}


	@Override
	public void leaveUnitTestExpect(UnitTestExpect ute) {
		IExpr x = fs.meth.voidExpr(fs.meth.callInterface(J.MOCKEXPECTATION, mock, "expect", fs.meth.stringConst(ute.method.var), fs.meth.arrayOf(J.OBJECT, args)));
		x.flush();
		sv.result(null);
	}
}
