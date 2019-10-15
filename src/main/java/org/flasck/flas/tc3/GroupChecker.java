package org.flasck.flas.tc3;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class GroupChecker extends LeafAdapter implements ResultAware {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private CurrentTCState state;
	private FunctionDefinition currentFunction;

	public GroupChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv, CurrentTCState state) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		this.state = state;
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		System.out.println("TC fn " + fn.name().uniqueName());
		sv.push(new FunctionChecker(errors, repository, sv, state));
		this.currentFunction = fn;
	}

	@Override
	public void result(Object r) {
		currentFunction.bindType((Type)r);
	}

	@Override
	public void leaveFunctionGroup(FunctionGroup grp) {
		System.out.println("Leave TC grp " + grp);
		sv.result(null);
	}
}
