package org.flasck.flas.parsedForm;

// This could possibly make a comeback as the thing we stick in Scope
// in order to tie everything together
// But for now it's not used
@Deprecated
public class PackageDefn implements ContainsScope {
	public final String name;
	public final Scope scope;

	public PackageDefn(Scope scope, String pn) {
		this.name = pn;
		this.scope = new Scope(scope);
	}

	@Override
	public Scope innerScope() {
		return scope;
	}
}
