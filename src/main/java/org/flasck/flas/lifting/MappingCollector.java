package org.flasck.flas.lifting;

import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.StandaloneDefn;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;

public interface MappingCollector {
	void recordNestedVar(FunctionIntro fi, ObjectActionHandler meth, VarPattern vp);
	void recordNestedVar(FunctionIntro fn, ObjectActionHandler meth, TypedPattern tp);
	void recordDependency(StandaloneDefn fn);
}
