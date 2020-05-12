package org.flasck.flas.resolver;

import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.RepositoryEntry;

public interface NestingChain {

	RepositoryEntry resolve(UnresolvedVar var);

}
