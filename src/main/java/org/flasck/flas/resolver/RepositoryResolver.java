package org.flasck.flas.resolver;

import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.RepositoryReader;

public class RepositoryResolver implements Resolver {
	private final RepositoryReader repository;

	public RepositoryResolver(RepositoryReader repository) {
		this.repository = repository;
	}

	@Override
	public void visitUnresolved(UnresolvedVar var) {
		var.bind(repository.get("test.repo.f"));
	}

}
