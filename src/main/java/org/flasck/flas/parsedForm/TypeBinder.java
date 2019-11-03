package org.flasck.flas.parsedForm;

import org.flasck.flas.tc3.Type;

public interface TypeBinder {
	void bindType(Type ty);
	Type type();
}
