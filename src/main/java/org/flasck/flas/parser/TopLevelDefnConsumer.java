package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.Scope;
import org.flasck.flas.parsedForm.StructDefn;

public interface TopLevelDefnConsumer extends ParsedLineConsumer {
	SolidName qualifyName(String base);
	void functionCase(FunctionCaseDefn o);
	void newStruct(StructDefn sd);
	
	@Deprecated // we should have all the relevant things in the right places ultimately
	Scope grabScope();
}
