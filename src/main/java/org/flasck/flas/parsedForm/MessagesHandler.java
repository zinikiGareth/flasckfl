package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.names.NameOfThing;

public interface MessagesHandler {
	void addMessage(MethodMessage o);
	IScope innerScope();
	NameOfThing caseName();
}
