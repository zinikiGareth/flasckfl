package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.HandlerImplements;

public interface FunctionScopeUnitConsumer extends FunctionIntroConsumer {
	void newHandler(HandlerImplements hi);
}
