package org.flasck.flas.parsedForm;

public class PackageDefn implements ContainsScope {
	public final String name;
	public final Scope scope = new Scope();

	public PackageDefn(String pn) {
		this.name = pn;
	}

	@Override
	public Scope innerScope() {
		return scope;
	}
}
