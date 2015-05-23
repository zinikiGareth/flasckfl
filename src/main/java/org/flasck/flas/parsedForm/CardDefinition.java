package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class CardDefinition {
	public final String name;
	public StateDefinition state;
	public List<TemplateLine> template;
	public final List<ContractImplements> contracts = new ArrayList<ContractImplements>();
	public final List<HandlerImplements> handlers = new ArrayList<HandlerImplements>();

	public CardDefinition(String name) {
		this.name = name;
	}

	public void addContractImplementation(ContractImplements o) {
		contracts.add(o);
	}

	public void addHandlerImplementation(HandlerImplements o) {
		handlers.add(o);
	}
}
