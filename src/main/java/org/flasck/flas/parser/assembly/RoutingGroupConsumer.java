package org.flasck.flas.parser.assembly;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.parsedForm.TypeReference;
import org.flasck.flas.parsedForm.UnresolvedVar;
import org.flasck.flas.parsedForm.assembly.CardBinding;
import org.flasck.flas.parsedForm.assembly.RoutingActions;

public interface RoutingGroupConsumer {
	CardBinding nameCard(UnresolvedVar var, TypeReference cardType);
	void assignCard(UnresolvedVar var, TypeReference cardType);
	void title(InputPosition pos, String s);
	void enter(RoutingActions actions);
	void at(RoutingActions at);
	void exit(RoutingActions actions);
	void route(RoutingGroupConsumer group);
	void isSecure();
	NameOfThing name();
	boolean cardDefnInScope(String s);
}
