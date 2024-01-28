package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.ObjectMethod;

@FunctionalInterface
public interface MethodConsumer {

	void addMethod(ObjectMethod method);

}
