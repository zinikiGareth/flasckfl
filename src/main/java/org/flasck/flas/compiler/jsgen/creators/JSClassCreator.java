package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.commonBase.names.NameOfThing;

public interface JSClassCreator {
	void justAnInterface();
	void inheritsFrom(NameOfThing baseClass, String javaName);
	void implementsJava(String clz);
	JSMethodCreator createMethod(String string, boolean prototype);
	JSMethodCreator constructor();
	void arg(String a);
	void field(NameOfThing type, String var);
}
