package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.names.SolidName;

public interface AccessorHolder {
	SolidName name();
	FieldAccessor getAccessor(String called);
}