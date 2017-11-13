package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.droidgen.VarHolder;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.IExpr;
import org.zinutils.bytecode.MethodDefiner;
import org.zinutils.bytecode.Var;

public interface GenerationContext<T> {

	NameOfThing nameContext();
	FunctionName funcName();
	boolean selectClass(String javaClassName);
	void defaultCtor();
	void instanceMethod();
	void staticMethod();
	void trampoline(String outerClz);
	void trampolineWithSelf(String outerClz);

	VarGenerator<T> generateVar();
	IntGenerator<T> generateInt();
	StringGenerator<T> generateString();
	TLVGenerator<T> generateTLV();
	ScopedVarGenerator<T> generateScopedVar();
	HandlerLambdaGenerator<T> generateHandlerLambda();
	FunctionDefnGenerator<T> generateFunctionDefn();
	StructDefnGenerator<T> generateStructDefn();
	ObjectDefnGenerator<T> generateObjectDefn();
	CardMemberGenerator<T> generateCardMember();
	CardGroupingGenerator<T> generateCardGrouping();
	ObjectReferenceGenerator<T> generateObjectReference();
	CardFunctionGenerator<T> generateCardFunction();
	BuiltinOpGenerator<T> generateBuiltinOp();
	PrimitiveTypeGenerator<T> generatePrimitiveType();

	ByteCodeSink getSink();
	Var getCxtArg();
	VarHolder getVarHolder();
	MethodDefiner getMethod();

	void beginClosure();
	void closureArg(Object visit);
	IExpr endClosure();
}
