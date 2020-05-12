package org.flasck.flas.resolver;

import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;

public interface NestingChain {
	RepositoryEntry resolve(UnresolvedVar var);
	NamedType type();
}
