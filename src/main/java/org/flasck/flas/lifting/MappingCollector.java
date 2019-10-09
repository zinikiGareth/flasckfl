package org.flasck.flas.lifting;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;

public interface MappingCollector {
	void recordNestedVar(FunctionDefinition fn, VarPattern vp);
	void recordNestedVar(FunctionDefinition fn, TypedPattern tp);
}
