package org.flasck.flas.parser;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.ScopeReceiver;
import org.flasck.flas.parsedForm.ContractDecl;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.StructDefn;
import org.flasck.flas.parsedForm.UnresolvedVar;

public interface ParsedLineConsumer {

	SolidName qualifyName(String base);
	void newStruct(StructDefn sd);
	void newContract(ContractDecl decl);
	void scopeTo(ScopeReceiver sendTo);
	void functionIntro(FunctionIntro o);
	void functionCase(FunctionCaseDefn o);
	void tupleDefn(List<UnresolvedVar> vars, Expr expr);

}
