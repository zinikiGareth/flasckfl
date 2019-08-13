package org.flasck.flas.compiler.jsgen;

public interface JSStorage {

	JSClassCreator newClass(String pkg, String clz);

	// Should we distinguish between methods and functions?
	JSMethodCreator newFunction(String string, String string2);

}
