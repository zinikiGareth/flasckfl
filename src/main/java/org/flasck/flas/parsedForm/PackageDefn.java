package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.parsedForm.Scope.ScopeEntry;

public class PackageDefn implements ContainsScope, Locatable {
	public final InputPosition location;
	public final String name;
	private final Scope scope;
	private final ScopeEntry myEntry;

	public PackageDefn(InputPosition location, Scope scope, String pn) {
		this.location = location;
		this.name = pn;
		myEntry = scope.define(pn, scope.fullName(pn), this);
		this.scope = new Scope(myEntry);
	}

	public PackageDefn(InputPosition location, PackageDefn from) {
		this.location = location;
		this.name = from.name;
		this.myEntry = from.scope.outerEntry;
		this.scope = new Scope(from.scope.outerEntry);
	}
	
	@Override
	public InputPosition location() {
		return location;
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
