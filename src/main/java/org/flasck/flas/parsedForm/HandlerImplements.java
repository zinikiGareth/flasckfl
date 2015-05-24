package org.flasck.flas.parsedForm;

import java.util.List;

public class HandlerImplements extends Implements {
	public final List<String> boundVars;

	public HandlerImplements(String type, List<String> boundVars) {
		super(type);
		this.boundVars = boundVars;
	}

}
