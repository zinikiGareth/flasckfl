package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.zinutils.bytecode.JavaInfo.Access;

public interface JSClassCreator {
	void notJS();
	boolean wantJS();
	void justAnInterface();
	void inheritsFrom(NameOfThing baseClass, String javaName);
	void implementsJava(String clz);
	JSMethodCreator createMethod(String string, boolean prototype);
	JSMethodCreator constructor();
	void field(boolean isStatic, Access access, NameOfThing type, String var);
	void field(boolean isStatic, Access access, NameOfThing type, String var, int value);
	void inheritsField(boolean isStatic, Access access, NameOfThing type, String var);
	NameOfThing name();
	void wantsTypeName();
	boolean hasField(String var);
}
