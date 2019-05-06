package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;

public interface FunctionIntroConsumer {
	FunctionName functionName(InputPosition location, String base);
	void functionIntro(FunctionIntro o);
	void functionCase(FunctionCaseDefn o);
}
