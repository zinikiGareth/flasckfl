package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.FunctionIntro;

public interface FunctionIntroConsumer {
	void functionIntro(FunctionIntro o);
	void moveOn();
	int nextCaseNumber(FunctionName fname);
}
