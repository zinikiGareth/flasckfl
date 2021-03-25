package org.flasck.flas.parser.assembly;

import org.flasck.flas.parsedForm.TypeReference;

public interface MainRoutingActionConsumer extends RoutingActionConsumer {
	public void provideMainCard(TypeReference main);
}
