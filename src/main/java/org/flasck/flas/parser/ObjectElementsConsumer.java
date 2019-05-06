package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.ObjectCtor;
import org.flasck.flas.parsedForm.StateDefinition;

public interface ObjectElementsConsumer {

	ObjectElementsConsumer defineState(StateDefinition state);

	ObjectElementsConsumer addConstructor(ObjectCtor ctor);

}
