package org.flasck.flas.tc3;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ut.UnitTestEvent;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class UTEventChecker extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final RepositoryReader repository;
	private final ErrorReporter errors;
	private final String fnCxt;

	public UTEventChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, String fnCxt, UnitTestEvent e) {
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
	public void leaveUnitTestEvent(UnitTestEvent e) {
		TypeChecker.resolveTargetZone(errors, repository, e.card.defn(), e.targetZone, "event", "send event to", true);
		sv.result(null);
	}
}
