package org.flasck.flas.parser;

import org.flasck.flas.parsedForm.FunctionIntro;

public interface FunctionIntroConsumer {
	void functionIntro(FunctionIntro o);
	void moveOn();
}
