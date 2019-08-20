package org.flasck.flas.tc3;

import org.flasck.flas.errors.ErrorResult;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.repository.LeafAdapter;
import org.flasck.flas.repository.Repository;

public class TypeChecker extends LeafAdapter {
	private final Repository repository;

	public TypeChecker(ErrorResult errors, Repository repository) {
		this.repository = repository;
	}

	@Override
	public void leaveFunction(FunctionDefinition fn) {
		fn.bindType(repository.get("Number"));
	}
}
