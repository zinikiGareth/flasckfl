package org.flasck.flas.types;

import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.NameOfThing;

public interface TypeWithMethods extends Locatable {
	String name();
	NameOfThing getTypeName();
	boolean hasMethod(String named);
	FunctionType getMethodType(String named);
}
