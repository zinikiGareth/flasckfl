package org.flasck.flas.rewrittenForm;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.Locatable;
import org.flasck.flas.parsedForm.PlatformSpec;

@SuppressWarnings("serial")
public class CardDefinition implements Locatable, Serializable {
	public final InputPosition kw;
	public final InputPosition location;
	public final String name;
	public StateDefinition state;
	public RWTemplate template;
	public final Map<String, PlatformSpec> platforms = new TreeMap<String, PlatformSpec>();
	public final List<RWContractImplements> contracts = new ArrayList<RWContractImplements>();
	public final List<RWContractService> services = new ArrayList<RWContractService>();
	public final List<RWHandlerImplements> handlers = new ArrayList<RWHandlerImplements>();

	public CardDefinition(InputPosition kw, InputPosition location, String name) {
		this.kw = kw;
		this.location = location;
		this.name = name;
	}

	@Override
	public InputPosition location() {
		return location;
	}

	public void addContractImplementation(RWContractImplements o) {
		contracts.add(o);
	}

	public void addContractService(RWContractService o) {
		services.add(o);
	}

	public void addHandlerImplementation(RWHandlerImplements o) {
		handlers.add(o);
	}
}
