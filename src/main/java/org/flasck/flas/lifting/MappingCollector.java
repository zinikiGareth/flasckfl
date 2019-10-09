package org.flasck.flas.lifting;

import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;

public interface MappingCollector {
	void recordNestedVar(FunctionIntro fi, VarPattern vp);
	void recordNestedVar(FunctionIntro fn, TypedPattern tp);
	void recordDependency(FunctionDefinition from, FunctionDefinition to);
}
