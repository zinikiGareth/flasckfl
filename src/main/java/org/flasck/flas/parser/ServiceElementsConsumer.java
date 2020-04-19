package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.RequiresContract;

public interface ServiceElementsConsumer extends HandlerBuilder {
	CardName cardName();
	void addRequiredContract(RequiresContract rc);
	void addProvidedService(Provides contractService);
}
