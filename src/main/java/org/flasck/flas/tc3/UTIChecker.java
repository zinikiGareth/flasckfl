package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;

public class UTIChecker extends LeafAdapter {
	private final NestedVisitor sv;
	private ErrorReporter errors;
	private RepositoryReader repository;

	public UTIChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		sv.push(this);
	}
	
	@Override
	public void visitExpr(Expr e, int nargs) {
		sv.push(new ExpressionChecker(errors, repository, new FunctionGroupTCState(repository, new DependencyGroup()), sv, false));
	}

	@Override
	public void leaveUnitTestInvoke(UnitTestInvoke uti) {
		sv.result(null);
	}
}
