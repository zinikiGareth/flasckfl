package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.ObjectMethod;

public interface FunctionScopeUnitConsumer extends FunctionIntroConsumer {
	void newHandler(HandlerImplements hi);
	void newStandaloneMethod(ObjectMethod meth);
}
