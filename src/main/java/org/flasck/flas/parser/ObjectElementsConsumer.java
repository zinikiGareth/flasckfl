package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectContract;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.Template;

public interface ObjectElementsConsumer extends MethodConsumer, HandlerBuilder {
	ObjectElementsConsumer defineState(StateDefinition state);
	ObjectElementsConsumer addTemplate(Template template);
	ObjectElementsConsumer addConstructor(ObjectCtor ctor);
	ObjectElementsConsumer addAccessor(ObjectAccessor acor);
	ObjectElementsConsumer requireContract(ObjectContract oc);
	void complete(ErrorReporter errors, InputPosition location);
	int templatePosn();
}
