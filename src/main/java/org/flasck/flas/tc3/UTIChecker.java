package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ut.UnitTestInvoke;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;

public class UTIChecker extends LeafAdapter {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private final String fnCxt;

	public UTIChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, String fnCxt) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.fnCxt = fnCxt;
		sv.push(this);
	}
	
	@Override
	public void visitExpr(Expr e, int nargs) {
		sv.push(new ExpressionChecker(errors, repository, new FunctionGroupTCState(repository, new DependencyGroup()), sv, fnCxt, false));
	}

	@Override
	public void leaveUnitTestInvoke(UnitTestInvoke uti) {
		sv.result(null);
	}
}
