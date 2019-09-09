package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.tc3.Type;

public interface WithTypeSignature {
	NameOfThing name();
	String signature();
	int argCount();
	Type type();
}
