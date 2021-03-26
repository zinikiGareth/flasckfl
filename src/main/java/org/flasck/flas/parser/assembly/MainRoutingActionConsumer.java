package org.flasck.flas.parser.assembly;

import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.RepositoryEntry;

public interface MainRoutingActionConsumer extends RoutingActionConsumer, RepositoryEntry {
	public void provideMainCard(TypeReference main);
	public void nameCard(UnresolvedVar var, TypeReference cardType);
}
