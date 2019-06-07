package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.parsedForm.StateDefinition;

public interface ServiceElementsConsumer {
	// TODO: I'm not really all that happy with this as a pattern
	// I'd prefer that people specifically received either a "NameOfThing" to use for the context
	// Or at least a specific namer
	CardName cardName();
	void defineState(StateDefinition stateDefinition);
}
