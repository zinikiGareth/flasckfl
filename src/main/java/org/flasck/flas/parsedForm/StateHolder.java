package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.names.NameOfThing;

public interface StateHolder {
	StateDefinition state();
	NameOfThing name();
}
