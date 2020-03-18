package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.CardName;
import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.parsedForm.ContractImplements;
import org.flasck.flas.parsedForm.Provides;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.Template;

public interface CardElementsConsumer {
	// TODO: I'm not really all that happy with this as a pattern
	// I'd prefer that people specifically received either a "NameOfThing" to use for the context
	// Or at least a specific namer
	CardName cardName();
	TemplateName templateName(String text);
	void defineState(StateDefinition stateDefinition);
	void addTemplate(Template template);
	void addEventHandler(ObjectMethod meth);
	void addProvidedService(Provides contractService);
	void addContractImplementation(ContractImplements contractImplements);
}
