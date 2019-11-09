package org.flasck.flas.generators;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.hsie.ClosureTraverser;
import org.flasck.flas.hsie.HSIGenerator;
import org.flasck.flas.vcode.hsieForm.ClosureHandler;
import org.flasck.flas.vcode.hsieForm.HSIEForm;
import org.flasck.flas.vcode.hsieForm.HSIEVisitor;
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

	ClosureHandler<T> getClosureHandler();
	HSIEVisitor<T> hsi(HSIGenerator<T> droidHSIGenerator, HSIEForm form, GenerationContext<T> cxt, ClosureTraverser<T> closGen);

	VarGenerator<T> generateVar();
	IntGenerator<T> generateInt();
	BoolGenerator<T> generateBool();
	DoubleGenerator<T> generateDouble();
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
	CSRGenerator<T> generateCSR();
	FuncGenerator<T> generateFunc();

	ByteCodeSink getSink();
	Var getCxtArg();
	MethodDefiner getMethod();

	void beginClosure();
	void closureArg(Object visit);
	IExpr endClosure();
}
