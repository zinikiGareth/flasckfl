package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class FunctionIntro {
	public final InputPosition location;
	public final String name;
	public final List<Object> args;

	public FunctionIntro(InputPosition location, String name, List<Object> args) {
		this.location = location;
		this.name = name;
		this.args = args;
	}

	@Override
	public String toString() {
		return "FI[" + name + "/" + args.size() + "]";
	}
}
