package org.flasck.flas.parsedForm;

import java.util.List;

public class CardDefinition {
	public final String name;
	public StateDefinition state;
	public List<TemplateLine> template;

	public CardDefinition(String name) {
		this.name = name;
	}

	public void addContractImplementation(ContractImplements o) {
		// TODO Auto-generated method stub
		
	}

	public void addHandlerImplementation(HandlerImplements o) {
		// TODO Auto-generated method stub
		
	}
}
