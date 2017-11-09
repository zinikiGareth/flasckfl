package org.flasck.flas.generators;

import java.util.List;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.zinutils.bytecode.ByteCodeSink;
import org.zinutils.bytecode.GenericAnnotator.PendingVar;
import org.zinutils.bytecode.MethodDefiner;

public interface GenerationContext {

	NameOfThing nameContext();
	void selectClass(String javaClassName);
	// TODO: withContext should be always true in the long run
	void instanceMethod(boolean withContext);
	boolean hasMethod();
	ByteCodeSink getSink();
	MethodDefiner getMethod();
	List<PendingVar> getVars();

}
