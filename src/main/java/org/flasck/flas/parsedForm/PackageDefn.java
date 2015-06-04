package org.flasck.flas.parsedForm;

public class PackageDefn implements ContainsScope {
	public final String name;
	private final Scope scope;

	public PackageDefn(Scope scope, String pn) {
		this.name = pn;
		this.scope = new Scope(scope);
	}

	@Override
	public Scope innerScope() {
		return scope;
	}
}
