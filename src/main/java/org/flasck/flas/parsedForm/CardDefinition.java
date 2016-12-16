package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.PlatformSpec;
import org.flasck.flas.commonBase.names.CardName;

public class CardDefinition implements ContainsScope, Locatable {
	public final InputPosition kw;
	public final InputPosition location;
	public final String simpleName;
	public final String name;
	public StateDefinition state;
	public final List<Template> templates = new ArrayList<Template>();
	// Used during the collection process, but eliminated by detox
	public final List<D3Thing> d3s = new ArrayList<D3Thing>();
	public final Map<String, PlatformSpec> platforms = new TreeMap<String, PlatformSpec>();
	public final List<ContractImplements> contracts = new ArrayList<ContractImplements>();
	public final List<ContractService> services = new ArrayList<ContractService>();
	public final List<HandlerImplements> handlers = new ArrayList<HandlerImplements>();
	public final Scope fnScope;
	public final CardName cardName;

	public CardDefinition(InputPosition kw, InputPosition location, IScope outer, CardName name) {
		this.kw = kw;
		this.location = location;
		this.simpleName = name.cardName;
		outer.define(simpleName, name.jsName(), this);
		this.cardName = name;
		this.name = name.jsName();
		this.fnScope = new Scope(name);
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
