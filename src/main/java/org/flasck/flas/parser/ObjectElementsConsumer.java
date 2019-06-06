package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StateDefinition;
import org.flasck.flas.parsedForm.Template;

public interface ObjectElementsConsumer {
	SolidName name();

	ObjectElementsConsumer defineState(StateDefinition state);

	ObjectElementsConsumer addTemplate(Template template);

	ObjectElementsConsumer addConstructor(ObjectCtor ctor);

	ObjectElementsConsumer addAccessor(ObjectAccessor acor);

	ObjectElementsConsumer addMethod(ObjectMethod acor);

}
