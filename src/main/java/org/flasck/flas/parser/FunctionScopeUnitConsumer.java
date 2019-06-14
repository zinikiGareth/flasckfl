package org.flasck.flas.parser;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectMethod;

public interface FunctionScopeUnitConsumer {
	void functionDefn(FunctionDefinition func);
	void tupleDefn(List<LocatedName> vars, FunctionName leadName, Expr expr);
	void newHandler(HandlerImplements hi);
	void newStandaloneMethod(ObjectMethod meth);
}
