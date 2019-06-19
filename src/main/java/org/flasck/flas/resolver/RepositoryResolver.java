package org.flasck.flas.resolver;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.UnresolvedOperator;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.repository.RepositoryReader;

public class RepositoryResolver implements Resolver {
	private final ErrorReporter errors;
	private final RepositoryReader repository;
	private NameOfThing scope;

	public RepositoryResolver(ErrorReporter errors, RepositoryReader repository) {
		this.errors = errors;
		this.repository = repository;
	}

	public void currentScope(NameOfThing scope) {
		this.scope = scope;
	}
	
	@Override
	public void visitUnresolvedVar(UnresolvedVar var) {
		final String prefix = scope != null ? scope.uniqueName() + "." : "";
		final RepositoryEntry defn = repository.get(prefix + var.var);
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
