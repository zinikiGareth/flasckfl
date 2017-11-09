package org.flasck.flas.generators;

import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.NameOfThing;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.MethodDefiner;

public interface GenerationContext {

	NameOfThing nameContext();
	FunctionName funcName();
	void selectClass(String javaClassName);
	void implementsInterface(String intf);
	// TODO: withContext should be always true in the long run
	void instanceMethod(boolean withContext);
	void staticMethod(boolean withContext);
	boolean hasMethod();
	ByteCodeSink getSink();
	MethodDefiner getMethod();
	List<PendingVar> getVars();

}
