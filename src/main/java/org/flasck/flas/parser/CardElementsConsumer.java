package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.Template;

public interface CardElementsConsumer {

	void defineState(StateDefinition stateDefinition);
	TemplateName templateName(String text);
	void addTemplate(Template template);

}
