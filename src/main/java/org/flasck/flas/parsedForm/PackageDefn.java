package org.flasck.flas.parsedForm;

import org.flasck.flas.parsedForm.Scope.ScopeEntry;

public class PackageDefn implements ContainsScope {
	public final String name;
	private final Scope scope;
	private final ScopeEntry myEntry;

	public PackageDefn(Scope scope, String pn) {
		this.name = pn;
		myEntry = scope.define(pn, scope.fullName(pn), this);
		this.scope = new Scope(myEntry);
	}

	public PackageDefn(PackageDefn from) {
		this.name = from.name;
		this.myEntry = from.scope.outerEntry;
		this.scope = new Scope(from.scope.outerEntry);
	}
	
	public void replaceOther() {
		this.scope.outerEntry.setValue(this);
	}

	public ScopeEntry myEntry() {
		return myEntry;
	}
	
	@Override
	public Scope innerScope() {
		return scope;
	}
}
