package org.flasck.flas.parsedForm;

public class PackageDefn implements ContainsScope {
	public final String name;
	private final Scope scope;

	public PackageDefn(Scope scope, String pn) {
		this.name = pn;
		this.scope = new Scope(scope.define(pn, scope.fullName(pn), this));
	}

	public PackageDefn(PackageDefn from) {
		this.name = from.name;
		this.scope = new Scope(from.scope.outerEntry);
	}
	
	public void replaceOther() {
		this.scope.outerEntry.setValue(this);
	}

	@Override
	public Scope innerScope() {
		return scope;
	}
}
