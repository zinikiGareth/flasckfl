package org.flasck.flas.parsedForm;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parser.AgentElementsConsumer;
import org.flasck.flas.repository.RepositoryEntry;
import org.flasck.flas.tc3.NamedType;
import org.flasck.flas.tc3.Type;
import org.zinutils.exceptions.NotImplementedException;

public class AgentDefinition implements Locatable, AgentElementsConsumer, RepositoryEntry, NamedType, StateHolder, ContractImplementor, ContractProvider, FieldsHolder {
	public final InputPosition kw;
	public final InputPosition location;
	public final String simpleName;
	private StateDefinition state;
	public final List<Template> templates = new ArrayList<Template>();
	public final List<ImplementsContract> contracts = new ArrayList<ImplementsContract>();
	public final List<RequiresContract> requires = new ArrayList<>();
	public final List<Provides> services = new ArrayList<Provides>();
	public final List<HandlerImplements> handlers = new ArrayList<HandlerImplements>();
	public final CardName name;

	public AgentDefinition(InputPosition kw, InputPosition location, CardName name) {
		this.kw = kw;
		this.location = location;
		this.simpleName = name.cardName;
		this.name = name;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public CardName name() {
		return name;
	}

	public CardName cardName() {
		return name;
	}

	@Override
	public void addProvidedService(Provides contractService) {
		this.services.add(contractService);
	}

	@Override
	public void defineState(StateDefinition stateDefinition) {
		this.state = stateDefinition;
	}

	public StateDefinition state() {
		return state;
	}

	public void addContractImplementation(ImplementsContract o) {
		contracts.add(o);
	}

	public void addRequiredContract(RequiresContract o) {
		requires.add(o);
	}

	public void addContractService(Provides o) {
		services.add(o);
	}

	public void newHandler(ErrorReporter errors, HandlerImplements o) {
		handlers.add(o);
	}

	@Override
	public ImplementsContract implementsContract(NameOfThing ctr) {
		for (ImplementsContract ic : this.contracts) {
			if (ic.implementsType().namedDefn().name().uniqueName().equals(ctr.uniqueName()))
				return ic;
		}
		return null;
	}

	@Override
	public Provides providesContract(NameOfThing ctr) {
		for (Provides ic : this.services) {
			if (ic.implementsType().namedDefn().name().uniqueName().equals(ctr.uniqueName()))
				return ic;
		}
		return null;
	}

	@Override
	public String toString() {
		return "Agent[" + this.cardName().uniqueName() + "]";
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
}
