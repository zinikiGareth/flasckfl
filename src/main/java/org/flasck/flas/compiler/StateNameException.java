package org.flasck.flas.compiler;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.VarName;

@SuppressWarnings("serial")
public class StateNameException extends RuntimeException {
	private final InputPosition location;

	public StateNameException(VarName name) {
		super("State Name: " + name.uniqueName());
		this.location = name.loc;
	}

	public InputPosition location() {
		return location;
	}

}
