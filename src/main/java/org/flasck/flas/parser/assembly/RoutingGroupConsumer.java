package org.flasck.flas.parser.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.assembly.RoutingActions;

public interface RoutingGroupConsumer {
	void assignCard(UnresolvedVar var, TypeReference cardType);
	void title(InputPosition pos, String s);
	void enter(RoutingActions actions);
	void exit(RoutingActions actions);
	void route(RoutingGroupConsumer group);
}
