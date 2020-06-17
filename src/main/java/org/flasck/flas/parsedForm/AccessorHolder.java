package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.names.NameOfThing;

public interface AccessorHolder {
	NameOfThing name();
	FieldAccessor getAccessor(String called);
}