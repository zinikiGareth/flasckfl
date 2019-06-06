package org.flasck.flas.parser;

import org.flasck.flas.blockForm.InputPosition;
import org.flasck.flas.commonBase.names.FunctionName;

public interface FunctionNameProvider {
	FunctionName functionName(InputPosition location, String base);
}
