package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

public class CardDefinition implements ContainsScope {
	public final String name;
	public StateDefinition state;
	public TemplateLine template;
	public final List<ContractImplements> contracts = new ArrayList<ContractImplements>();
	public final List<HandlerImplements> handlers = new ArrayList<HandlerImplements>();
	public final Scope fnScope;

	public CardDefinition(Scope s, String name) {
		this.name = name;
		this.fnScope = new Scope(s);
	}

	public void addContractImplementation(ContractImplements o) {
		contracts.add(o);
	}

	public void addHandlerImplementation(HandlerImplements o) {
		handlers.add(o);
	}

	@Override
	public Scope innerScope() {
		return fnScope;
	}
}
