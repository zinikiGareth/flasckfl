package org.flasck.flas.tc3;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.NestedVisitor;
import org.flasck.flas.repository.RepositoryReader;
import org.flasck.flas.repository.ResultAware;

public class TypeChecker extends LeafAdapter implements ResultAware {
	private final RepositoryReader repository;
	private final NestedVisitor sv;
	private Type type;

	public TypeChecker(ErrorReporter errors, RepositoryReader repository, NestedVisitor sv) {
		this.repository = repository;
		this.sv = sv;
		sv.push(this);
	}

	@Override
	public void visitFunction(FunctionDefinition fn) {
		sv.push(new ExpressionChecker(repository, sv));
	}
	
	
	@Override
	public void result(Object r) {
		this.type = (Type) r;
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		fn.bindType(type);
	}
}
