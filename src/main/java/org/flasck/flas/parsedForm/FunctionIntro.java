package org.flasck.flas.parsedForm;

import java.util.List;

public class FunctionIntro {
	public final String name;
	public final List<Object> args;

	public FunctionIntro(String name, List<Object> args) {
		this.name = name;
		this.args = args;
	}
	
	@Override
	public String toString() {
		return "FI[" + name + "/" + args.size() + "]";
	}
}
