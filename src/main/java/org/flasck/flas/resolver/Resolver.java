package org.flasck.flas.resolver;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.repository.RepositoryVisitor;

public interface Resolver extends RepositoryVisitor {

	void currentScope(NameOfThing scope);

	void resolveAll();
}
