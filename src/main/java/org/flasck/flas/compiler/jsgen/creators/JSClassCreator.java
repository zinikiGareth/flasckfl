package org.flasck.flas.compiler.jsgen.creators;

public interface JSClassCreator {

	JSMethodCreator createMethod(String string, boolean prototype);
	JSBlockCreator constructor();
}
