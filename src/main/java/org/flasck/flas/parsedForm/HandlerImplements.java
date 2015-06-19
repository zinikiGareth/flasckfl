package org.flasck.flas.parsedForm;

import java.util.List;

import org.flasck.flas.blockForm.InputPosition;

public class HandlerImplements extends Implements {
	public final String name;
	public final List<String> boundVars;

	public HandlerImplements(InputPosition location, String named, String type, List<String> boundVars) {
		super(location, type);
		this.name = named;
		this.boundVars = boundVars;
	}

}
