package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;

public interface FunctionScopeNamer extends FunctionNameProvider, HandlerNameProvider {
	FunctionName functionCase(InputPosition pos, String x, int caseNum);
}
