package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.droidgen.VarHolder;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public interface GenerationContext {

	NameOfThing nameContext();
	FunctionName funcName();
	boolean selectClass(String javaClassName);
	void defaultCtor();
	void instanceMethod();
	void staticMethod();
	void trampoline(String outerClz);
	void trampolineWithSelf(String outerClz);

	ByteCodeSink getSink();
	Var getCxtArg();
	VarHolder getVarHolder();
	MethodDefiner getMethod();

	void beginClosure();
	void closureArg(Object visit);
	IExpr endClosure();
}
