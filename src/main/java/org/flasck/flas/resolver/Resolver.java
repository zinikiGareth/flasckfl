package org.flasck.flas.resolver;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.repository.Repository;

public interface Resolver extends Repository.Visitor {

	void currentScope(NameOfThing scope);
}
