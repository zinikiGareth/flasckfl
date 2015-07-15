package org.flasck.flas.dom;

import org.zinutils.collections.ListMap;

public class UpdateTree {
	public static class Update {
		public final Route routeChanges;
		public final String updateType;
		public final String list;

		public Update(Route route, String updateType, String list) {
			this.routeChanges = route;
			this.updateType = updateType;
			this.list = list;
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
