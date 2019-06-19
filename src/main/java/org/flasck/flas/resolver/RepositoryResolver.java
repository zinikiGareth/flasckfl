package org.flasck.flas.resolver;

import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.RepositoryReader;

public class RepositoryResolver implements Resolver {
	private final RepositoryReader repository;

	public RepositoryResolver(RepositoryReader repository) {
		this.repository = repository;
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var) {
		var.bind(repository.get("test.repo.f"));
	}

	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator) {
		operator.bind(repository.get("++"));
	}

}
