package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ut.UnitTestInput;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class UTInputChecker extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final RepositoryReader repository;
	private final ErrorReporter errors;
	private final String fnCxt;

	public UTInputChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, String fnCxt, UnitTestInput e) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.fnCxt = fnCxt;
		sv.push(this);
	}
	
	@Override
	public void visitExpr(Expr expr, int nArgs) {
		sv.push(new ExpressionChecker(errors, repository, new FunctionGroupTCState(repository, new DependencyGroup()), sv, fnCxt, false));
	}

	@Override
	public void result(Object r) {
	}

	@Override
	public void leaveUnitTestInput(UnitTestInput e) {
		TypeChecker.resolveTargetZone(errors, repository, e.card.defn(), e.targetZone, "input", "process input data", true);
		sv.result(null);
	}
}
