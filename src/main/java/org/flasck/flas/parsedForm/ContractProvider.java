package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.names.NameOfThing;

public interface ContractProvider {
	boolean providesContract(NameOfThing ctr);
}
