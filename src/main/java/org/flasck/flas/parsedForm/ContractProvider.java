package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.names.NameOfThing;

public interface ContractProvider {
	Provides providesContract(NameOfThing ctr);
}
