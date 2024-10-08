package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.names.NameOfThing;

public interface RequiresHolder {
	Iterable<RequiresContract> requires();
	Provides providesContract(NameOfThing name);
}
