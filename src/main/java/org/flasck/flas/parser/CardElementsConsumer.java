package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.TemplateName;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.Template;

public interface CardElementsConsumer extends AgentElementsConsumer {
	// TODO: I'm not really all that happy with this as a pattern
	// I'd prefer that people specifically received either a "NameOfThing" to use for the context
	// Or at least a specific namer
	TemplateName templateName(String text);
	void addTemplate(Template template);
	void addEventHandler(ObjectMethod meth);
}
