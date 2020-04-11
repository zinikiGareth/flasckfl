package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.commonBase.names.NameOfThing;

public interface JSClassCreator {
	void inheritsFrom(NameOfThing baseClass);
	JSMethodCreator createMethod(String string, boolean prototype);
	JSBlockCreator constructor();
	void arg(String a);
}
