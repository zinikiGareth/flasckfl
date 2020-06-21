package org.flasck.flas.parsedForm;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.tc3.Type;

public interface TypeBinder {
	NameOfThing name();
	InputPosition location();
	void bindType(Type ty);
	Type type();
	int argCount();
}
