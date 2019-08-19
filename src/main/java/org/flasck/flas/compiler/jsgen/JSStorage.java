package org.flasck.flas.compiler.jsgen;

import java.io.File;

public interface JSStorage {

	JSClassCreator newClass(String pkg, String clz);

	// Should we distinguish between methods and functions?
	JSMethodCreator newFunction(String string, String string2);

	Iterable<File> files();

}
