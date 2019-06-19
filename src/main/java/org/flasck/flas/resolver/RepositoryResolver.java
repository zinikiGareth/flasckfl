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
		RepositoryEntry defn = find(scope, var.var);
		if (defn == null) {
			errors.message(var.location, "cannot resolve '" + var.var + "'");
			return;
		}
		var.bind(defn);
	}

	private RepositoryEntry find(NameOfThing s, String var) {
		if (s == null) {
			return repository.get(var);
		}
		final RepositoryEntry defn = repository.get(s.uniqueName() + "." + var);
		if (defn != null)
			return defn;
		else
			return find(s.container(), var);
	}

	@Override
	public void visitUnresolvedOperator(UnresolvedOperator operator) {
		operator.bind(repository.get("++"));
	}

}
