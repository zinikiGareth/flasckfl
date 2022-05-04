package org.flasck.flas.tc3;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.lifting.DependencyGroup;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.ut.UnitTestExpect;
import org.flasck.flas.parsedForm.ut.UnitTestExpectCancel;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;
import org.flasck.flas.tc3.ExpressionChecker.ExprResult;

public class ExpectCancelChecker extends LeafAdapter implements ResultAware {
	private final NestedVisitor sv;
	private final ErrorReporter errors;
	private final CurrentTCState state;
	private Type mock;
	private Type handler;

	public ExpectCancelChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, String fnCxt, UnitTestExpectCancel e) {
		this.errors = errors;
		this.sv = sv;
		sv.push(this);
		state = new FunctionGroupTCState(repository, new DependencyGroup());
		sv.push(new ExpressionChecker(errors, repository, state, sv, fnCxt, false));
	}
	
	@Override
	public void result(Object r) {
		mock = ((ExprResult)r).type;
	}
	
	@Override
	public void leaveUnitTestExpect(UnitTestExpect e) {
		// Check for cascades
		if (mock instanceof ErrorType || handler instanceof ErrorType) {
			sv.result(mock);
			return;
		}
		if (!(mock instanceof ContractDecl)) {
			errors.message(e.ctr.location(), "expect requires a contract variable");
			sv.result(null);
			return;
		}
		sv.result(null);
	}
}
