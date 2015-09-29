package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;
import org.flasck.flas.stories.FLASStory.State;

public class CardDefinition implements ContainsScope, Locatable {
	public final InputPosition location;
	public final String name;
	public StateDefinition state;
	public Template template;
	public final Map<String, PlatformSpec> platforms = new TreeMap<String, PlatformSpec>();
	public final List<ContractImplements> contracts = new ArrayList<ContractImplements>();
	public final List<ContractService> services = new ArrayList<ContractService>();
	public final List<HandlerImplements> handlers = new ArrayList<HandlerImplements>();
	public final Scope fnScope;

	public CardDefinition(InputPosition location, Scope outer, String name) {
		this.location = location;
		ScopeEntry se = outer.define(State.simpleName(name), name, this);
		this.name = name;
		this.fnScope = new Scope(se);
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public void addContractImplementation(ContractImplements o) {
		contracts.add(o);
	}

	public void addContractService(ContractService o) {
		services.add(o);
	}

	public void addHandlerImplementation(HandlerImplements o) {
		handlers.add(o);
	}

	@Override
	public Scope innerScope() {
		return fnScope;
	}
}
