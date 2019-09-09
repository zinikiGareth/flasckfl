package org.flasck.flas.tc3;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.Repository;
import org.flasck.flas.repository.RepositoryReader;

public class TypeChecker extends LeafAdapter {
	private final RepositoryReader repository;
	private final NestedVisitor sv;

	public TypeChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv) {
		this.repository = repository;
		this.sv = sv;
		sv.push(this);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		sv.push(new ExpressionChecker());
	}
	
	@Override
	public void leaveFunction(FunctionDefinition fn) {
		fn.bindType(repository.get("Number"));
	}
}
