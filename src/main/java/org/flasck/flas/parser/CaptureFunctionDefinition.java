package org.flasck.flas.parser;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.StandaloneMethod;
import org.flasck.flas.parsedForm.TypedPattern;
import org.flasck.flas.parsedForm.VarPattern;

public class CaptureFunctionDefinition implements FunctionScopeUnitConsumer {
	private final FunctionScopeUnitConsumer topLevel;
	private final FunctionDefnConsumer consumer;

	public CaptureFunctionDefinition(FunctionScopeUnitConsumer topLevel, FunctionDefnConsumer consumer) {
		this.topLevel = topLevel;
		this.consumer = consumer;
	}

	public void functionDefn(FunctionDefinition func) {
		consumer.functionDefn(func);
		topLevel.functionDefn(func);
	}

	public void tupleDefn(List<LocatedName> vars, FunctionName leadName, Expr expr) {
		topLevel.tupleDefn(vars, leadName, expr);
	}

	public void newHandler(HandlerImplements hi) {
		topLevel.newHandler(hi);
	}

	public void newStandaloneMethod(StandaloneMethod meth) {
		topLevel.newStandaloneMethod(meth);
	}

	public void newObjectMethod(ObjectActionHandler om) {
		topLevel.newObjectMethod(om);
	}

	public void argument(VarPattern parm) {
		topLevel.argument(parm);
	}

	public void argument(TypedPattern with) {
		topLevel.argument(with);
	}
}
