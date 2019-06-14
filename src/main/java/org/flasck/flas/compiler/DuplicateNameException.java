package org.flasck.flas.compiler;

import org.flasck.flas.commonBase.names.NameOfThing;

@SuppressWarnings("serial")
public class DuplicateNameException extends RuntimeException {

	public DuplicateNameException(NameOfThing name) {
		super("Duplicate Name: " + name.uniqueName());
	}

}
