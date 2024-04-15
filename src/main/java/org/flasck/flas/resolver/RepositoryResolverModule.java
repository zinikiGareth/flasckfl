package org.flasck.flas.resolver;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.repository.RepositoryReader;

public interface RepositoryResolverModule {
	void init(ErrorReporter errors, RepositoryReader repository);
	void currentScope(NameOfThing scope);
}
