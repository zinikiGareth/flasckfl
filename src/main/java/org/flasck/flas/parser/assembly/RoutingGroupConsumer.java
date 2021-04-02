package org.flasck.flas.parser.assembly;

import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.assembly.RoutingActions;

public interface RoutingGroupConsumer {
	void assignCard(UnresolvedVar var, TypeReference cardType);
	void enter(RoutingActions actions);
	void exit(RoutingActions actions);
	void route(RoutingGroupConsumer group);
}
