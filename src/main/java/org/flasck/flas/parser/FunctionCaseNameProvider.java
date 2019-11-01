package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;

public interface FunctionCaseNameProvider {
	FunctionName functionCaseName(InputPosition location, String base, int caseNum);
}
