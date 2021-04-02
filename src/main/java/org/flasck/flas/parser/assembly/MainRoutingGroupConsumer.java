package org.flasck.flas.parser.assembly;

import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.repository.RepositoryEntry;

public interface MainRoutingGroupConsumer extends RoutingGroupConsumer, RepositoryEntry {
	public void provideMainCard(TypeReference main);
	public void nameCard(UnresolvedVar var, TypeReference cardType);
}
