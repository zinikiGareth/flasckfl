package org.flasck.flas.parsedForm;

public class D3Invoke implements TemplateLine {
	public final Scope scope;
	public final D3Thing d3;

	public D3Invoke(Scope scope, D3Thing d3) {
		this.scope = scope;
		this.d3 = d3;
	}

}
