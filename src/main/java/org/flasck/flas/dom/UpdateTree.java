package org.flasck.flas.dom;

import org.zinutils.collections.ListMap;

public class UpdateTree {
	public static class Update {
		public final String routeChanges;
		public final String updateType;

		public Update(String routeChanges, String updateType) {
			this.routeChanges = routeChanges.length() == 0 ? "" : routeChanges.substring(1);
			this.updateType = updateType;
		}
		
		@Override
		public String toString() {
			return "("+routeChanges+":"+updateType+")";
		}
	}

	public final String prefix;
	public final ListMap<String, Update> updates;

	public UpdateTree(String prefix, ListMap<String, Update> updates) {
		this.prefix = prefix;
		this.updates = updates;
	}
	
	@Override
	public String toString() {
		return prefix + ": " + updates;
	}

}
