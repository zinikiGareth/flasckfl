package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.PlatformSpec;
import org.flasck.flas.commonBase.names.CSName;
import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.HandlerName;
import org.flasck.flas.commonBase.names.NamedThing;
import org.flasck.flas.commonBase.names.SolidName;

public class CardGrouping implements NamedThing {
	public static class ContractGrouping {
		public final CSName implName;
		public final String referAsVar;
		public final SolidName contractName;

		public ContractGrouping(SolidName type, CSName implName, String referAsVar) {
			this.contractName = type;
			this.implName = implName;
			this.referAsVar = referAsVar;
		}
	}
	
	public static class ServiceGrouping {
		public final String type;
		public final CSName implName;
		public final String referAsVar;

		public ServiceGrouping(String type, CSName implName, String referAsVar) {
			this.type = type;
			this.implName = implName;
			this.referAsVar = referAsVar;
		}
	}
	
	public static class HandlerGrouping {
		public final HandlerName type;
		public final RWHandlerImplements impl;

		public HandlerGrouping(HandlerName type, RWHandlerImplements impl) {
			this.type = type;
			this.impl = impl;
		}
	}
	
	private final InputPosition location;
	private final CardName cardName;
	public final RWStructDefn struct;
	public final List<ContractGrouping> contracts = new ArrayList<ContractGrouping>();
	public final List<ServiceGrouping> services = new ArrayList<ServiceGrouping>();
	public final List<HandlerGrouping> handlers = new ArrayList<HandlerGrouping>();
	public final Map<String, PlatformSpec> platforms = new TreeMap<String, PlatformSpec>();
	public final List<RWEventHandler> areaActions = new ArrayList<>();
	
	public CardGrouping(InputPosition location, CardName name, RWStructDefn struct) {
		this.location = location;
		this.cardName = name;
		this.struct = struct;
	}

	@Override
	public InputPosition location() {
		return location;
	}
	
	public CardName getName() {
		return cardName;
	}

	public CardName name() {
		return cardName;
	}
}
