package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.parsedForm.ImplementsContract;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;
import org.flasck.flas.parsedForm.StateDefinition;

public interface AgentElementsConsumer extends HandlerBuilder {
	CardName cardName();
	void defineState(StateDefinition stateDefinition);
	void addProvidedService(Provides contractService);
	void addContractImplementation(ImplementsContract contractImplements);
	void addRequiredContract(RequiresContract rc);
}
