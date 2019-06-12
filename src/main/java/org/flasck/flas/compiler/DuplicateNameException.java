package org.flasck.flas.compiler;

import org.flasck.flas.commonBase.names.NameOfThing;

@SuppressWarnings("serial")
public class DuplicateNameException extends RuntimeException {
	private final NameOfThing name;

	public DuplicateNameException(NameOfThing name) {
		this.name = name;
	}

}
