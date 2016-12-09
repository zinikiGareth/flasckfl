package org.flasck.flas.rewrittenForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.flasck.flas.commonBase.PlatformSpec;

public class CardGrouping {
	public static class ContractGrouping {
		public final String type;
		public final CSName implName;
		public final String referAsVar;

		public ContractGrouping(String type, CSName implName, String referAsVar) {
			this.type = type;
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
		public final String type;
		public final RWHandlerImplements impl;

		public HandlerGrouping(String type, RWHandlerImplements impl) {
			this.type = type;
			this.impl = impl;
		}
	}
	
	private final CardName cardName;
	public final RWStructDefn struct;
	public final List<ContractGrouping> contracts = new ArrayList<ContractGrouping>();
	public final List<ServiceGrouping> services = new ArrayList<ServiceGrouping>();
	public final List<HandlerGrouping> handlers = new ArrayList<HandlerGrouping>();
	public final Map<String, PlatformSpec> platforms = new TreeMap<String, PlatformSpec>();
	
	public CardGrouping(CardName name, RWStructDefn struct) {
		this.cardName = name;
		this.struct = struct;
	}

	public CardName name() {
		return cardName;
	}
}
