package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.ServiceElementsConsumer;

public class ServiceDefinition implements ContainsScope, Locatable, ServiceElementsConsumer {
	public final InputPosition kw;
	public final InputPosition location;
	public final String simpleName;
	public StateDefinition state;
	public final List<ContractService> services = new ArrayList<ContractService>();
	public final List<HandlerImplements> handlers = new ArrayList<HandlerImplements>();
	public final Scope fnScope;
	public final CardName serviceName;

	public ServiceDefinition(ErrorReporter errors, InputPosition kw, InputPosition location, IScope outer, CardName name) {
		this.kw = kw;
		this.location = location;
		this.simpleName = name.cardName;
		outer.define(errors, simpleName, this);
		this.serviceName = name;
		this.fnScope = new Scope(name);
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public CardName cardName() {
		return serviceName;
	}

	@Override
	public void defineState(StateDefinition stateDefinition) {
		this.state = stateDefinition;
	}

	public void addProvidedService(ContractService contractService) {
		services.add(contractService);
	}

	public void addContractService(ContractService o) {
		services.add(o);
	}

	public void addHandlerImplementation(HandlerImplements o) {
		handlers.add(o);
	}

	@Override
	public IScope innerScope() {
		return fnScope;
	}
}
