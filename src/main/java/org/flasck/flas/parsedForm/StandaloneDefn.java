package org.flasck.flas.parsedForm;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.lifting.NestedVarReader;

public interface StandaloneDefn {
	FunctionName name();
	NestedVarReader nestedVars();
}
