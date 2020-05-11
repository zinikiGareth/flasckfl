package org.flasck.flas.parser;

import java.util.List;

import org.flasck.flas.commonBase.Expr;
import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.errors.ErrorReporter;
import org.flasck.flas.parsedForm.FunctionDefinition;
import org.flasck.flas.parsedForm.HandlerImplements;
import org.flasck.flas.parsedForm.LocatedName;
import org.flasck.flas.parsedForm.ObjectActionHandler;
import org.flasck.flas.parsedForm.PolyType;
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

	public void functionDefn(ErrorReporter errors, FunctionDefinition func) {
		consumer.functionDefn(errors, func);
	}

	public void tupleDefn(ErrorReporter errors, List<LocatedName> vars, FunctionName leadName, FunctionName pkgName, Expr expr) {
		topLevel.tupleDefn(errors, vars, leadName, pkgName, expr);
	}

	public void newHandler(ErrorReporter errors, HandlerImplements hi) {
		topLevel.newHandler(errors, hi);
	}

	public void newStandaloneMethod(ErrorReporter errors, StandaloneMethod meth) {
		topLevel.newStandaloneMethod(errors, meth);
	}

	public void newObjectMethod(ErrorReporter errors, ObjectActionHandler om) {
		topLevel.newObjectMethod(errors, om);
	}

	public void argument(ErrorReporter errors, VarPattern parm) {
		topLevel.argument(errors, parm);
	}

	public void argument(ErrorReporter errors, TypedPattern with) {
		topLevel.argument(errors, with);
	}

	@Override
	public void polytype(ErrorReporter errors, PolyType pt) {
		topLevel.polytype(errors, pt);
	}
}
