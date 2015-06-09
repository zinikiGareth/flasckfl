package org.flasck.flas.parsedForm;

import org.flasck.flas.parsedForm.Scope.ScopeEntry;

public class AbsoluteVar implements ExternalRef {
	public final String id;
	public final Object defn;

	@Deprecated // or at least should have a defn
	public AbsoluteVar(String id) {
		this.id = id;
		this.defn = null; // not a good idea
	}

	public AbsoluteVar(ScopeEntry entry) {
		this.id = entry.getKey();
		this.defn = entry.getValue();
	}
	
	@Override
	public String uniqueName() {
		return id;
	}
	
	@Override
	public int compareTo(Object o) {
		return this.toString().compareTo(o.toString());
	}

	@Override
	public String toString() {
		return id;
	}
}
