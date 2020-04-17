package org.flasck.flas.parser;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;

public interface FunctionScopeUnitConsumer extends FunctionDefnConsumer, HandlerConsumer {
	void tupleDefn(ErrorReporter errors, List<LocatedName> vars, FunctionName leadName, FunctionName pkgName, Expr expr);
	void newStandaloneMethod(ErrorReporter errors, StandaloneMethod meth);
	void newObjectMethod(ErrorReporter errors, ObjectActionHandler om);
	void argument(ErrorReporter errors, VarPattern parm);
	void argument(ErrorReporter errors, TypedPattern with);
}
