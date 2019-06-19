package org.flasck.flas.resolver;

import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;

public class RepositoryResolver implements Resolver {
	private final ErrorReporter errors;
	private final RepositoryReader repository;

	public RepositoryResolver(ErrorReporter errors, RepositoryReader repository) {
		this.errors = errors;
		this.repository = repository;
	}

	@Override
	public void visitUnresolvedVar(UnresolvedVar var) {
		final RepositoryEntry defn = repository.get("test.repo.f");
		if (defn == null) {
			errors.message(var.location, "cannot resolve '" + var.var + "'");
			return;
		}
		var.bind(defn);
	}

	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator) {
		operator.bind(repository.get("++"));
	}

}
