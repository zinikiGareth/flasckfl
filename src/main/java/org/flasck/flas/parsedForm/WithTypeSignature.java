package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.names.NameOfThing;

public interface WithTypeSignature {
	NameOfThing name();
	String signature();
	int argCount();
}
