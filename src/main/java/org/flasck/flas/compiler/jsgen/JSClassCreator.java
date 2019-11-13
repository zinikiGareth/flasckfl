package org.flasck.flas.compiler.jsgen;

public interface JSClassCreator {

	JSMethodCreator createMethod(String string, boolean prototype);
	JSBlockCreator constructor();
}
