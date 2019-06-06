package org.flasck.flas.parser;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.FunctionCaseDefn;
import org.flasck.flas.parsedForm.FunctionIntro;
import org.flasck.flas.parsedForm.LocatedName;

public interface FunctionIntroConsumer {
	void tupleDefn(List<LocatedName> vars, FunctionName leadName, Expr expr);
	void functionIntro(FunctionIntro o);
	void functionCase(FunctionCaseDefn o);
}
