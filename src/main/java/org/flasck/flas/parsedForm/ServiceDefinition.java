package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.parser.ServiceElementsConsumer;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class ServiceDefinition implements Locatable, ServiceElementsConsumer, RepositoryEntry, NamedType {
	public final InputPosition kw;
	public final InputPosition location;
	public final String simpleName;
	public StateDefinition state;
	public final List<Provides> services = new ArrayList<Provides>();
	public final List<HandlerImplements> handlers = new ArrayList<HandlerImplements>();
	public final CardName serviceName;

	public ServiceDefinition(InputPosition kw, InputPosition location, CardName name) {
		this.kw = kw;
		this.location = location;
		this.simpleName = name.cardName;
		this.serviceName = name;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public CardName name() {
		return serviceName;
	}
	
	public CardName cardName() {
		return serviceName;
	}

	@Override
	public void defineState(StateDefinition stateDefinition) {
		this.state = stateDefinition;
	}

	public void addProvidedService(Provides contractService) {
		services.add(contractService);
	}

	public void addContractService(Provides o) {
		services.add(o);
	}

	public void addHandlerImplementation(HandlerImplements o) {
		handlers.add(o);
	}

	@Override
	public void dumpTo(PrintWriter pw) {
		pw.println(toString());
	}
	
	@Override
	public String signature() {
		throw new NotImplementedException();
	}

	@Override
	public int argCount() {
		throw new NotImplementedException();
	}

	@Override
	public Type get(int pos) {
		throw new NotImplementedException();
	}

	@Override
	public boolean incorporates(InputPosition pos, Type other) {
		throw new NotImplementedException();
	}

	@Override
	public String toString() {
		return "Service[" + serviceName.uniqueName() + "]";
	}
}
