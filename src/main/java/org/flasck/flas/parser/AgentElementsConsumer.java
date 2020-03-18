package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.StateDefinition;

public interface AgentElementsConsumer {
	CardName cardName();
	void defineState(StateDefinition stateDefinition);
	void addProvidedService(Provides contractService);
	void addContractImplementation(ContractImplements contractImplements);
}
