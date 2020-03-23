package org.flasck.flas.types;

import org.flasck.flas.commonBase.Locatable;
import org.flasck.flas.commonBase.names.NameOfThing;

public interface TypeWithMethods extends Locatable {
	String nameAsString();
	NameOfThing getTypeName();
	boolean hasMethod(String named);
}
