package org.flasck.flas.parsedForm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardGrouping {
	public static class ContractGrouping {
		public final String type;
		public final String implName;
		public final String referAsVar;

		public ContractGrouping(String type, String implName, String referAsVar) {
			this.type = type;
			this.implName = implName;
			this.referAsVar = referAsVar;
		}
	}
	
	public static class ServiceGrouping {
		public final String type;
		public final String implName;
		public final String referAsVar;

		public ServiceGrouping(String type, String implName, String referAsVar) {
			this.type = type;
			this.implName = implName;
			this.referAsVar = referAsVar;
		}
	}
	
	public static class HandlerGrouping {
		public final String type;
		public final HandlerImplements impl;

		public HandlerGrouping(String type, HandlerImplements impl) {
			this.type = type;
			this.impl = impl;
		}
	}
	
	public final StructDefn struct;
	public final Map<String, Object> inits = new HashMap<String, Object>();
	public final List<ContractGrouping> contracts = new ArrayList<ContractGrouping>();
	public final List<ServiceGrouping> services = new ArrayList<ServiceGrouping>();
	public final List<HandlerGrouping> handlers = new ArrayList<HandlerGrouping>();
	
	public CardGrouping(StructDefn struct) {
		this.struct = struct;
	}
}
