package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.parsedForm.Provides;

public interface ServiceElementsConsumer extends HandlerBuilder {
	CardName cardName();
	void addProvidedService(Provides contractService);
}
