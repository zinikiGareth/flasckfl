package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.ObjectAccessor;
import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.ObjectMethod;
import org.flasck.flas.parsedForm.StateDefinition;

public interface ObjectElementsConsumer {
	SolidName name();

	ObjectElementsConsumer defineState(StateDefinition state);

	ObjectElementsConsumer addConstructor(ObjectCtor ctor);

	ObjectElementsConsumer addAccessor(ObjectAccessor acor);

	ObjectElementsConsumer addMethod(ObjectMethod acor);
}
