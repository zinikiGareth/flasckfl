package org.flasck.flas.parser;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;

public interface FunctionScopeUnitConsumer extends FunctionDefnConsumer, HandlerConsumer {
	void tupleDefn(List<LocatedName> vars, FunctionName leadName, FunctionName pkgName, Expr expr);
	void newStandaloneMethod(StandaloneMethod meth);
	void newObjectMethod(ObjectActionHandler om);
	void argument(VarPattern parm);
	void argument(TypedPattern with);
}
