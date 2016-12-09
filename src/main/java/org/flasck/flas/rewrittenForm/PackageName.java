package org.flasck.flas.rewrittenForm;

public class PackageName implements NameOfThing {
	private final String name;

	public PackageName(String s) {
		this.name = s;
	}
	
	@Override
	public String jsName() {
		return name;
	}

	public String simpleName() {
		return name;
	}

}
