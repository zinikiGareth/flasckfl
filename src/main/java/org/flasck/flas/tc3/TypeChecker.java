package org.flasck.flas.tc3;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.FunctionGroup;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;

public class TypeChecker extends LeafAdapter {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private final NestedVisitor sv;

	public TypeChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv) {
		this.errors = errors;
		this.repository = repository;
		this.sv = sv;
		sv.push(this);
	}

	@Override
	public void visitFunctionGroup(FunctionGroup grp) {
		sv.push(new GroupChecker(errors, repository, sv, new FunctionGroupTCState(repository)));
	}
}
