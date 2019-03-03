package org.flasck.flas.parser;

import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;

public interface ParsedLineConsumer {

	SolidName qualifyName(String base);
	void newStruct(StructDefn sd);
	void newContract(ContractDecl decl);
	void scopeTo(ScopeReceiver sendTo);
	void functionIntro(FunctionIntro o);
	void functionCase(FunctionCaseDefn o);

}
