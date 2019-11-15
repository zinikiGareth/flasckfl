package org.flasck.flas.compiler.jsgen.packaging;

import java.io.File;

import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;

public interface JSStorage {

	JSClassCreator newClass(String pkg, String clz);

	void ensurePackageExists(String filePkg, String pkg);

	JSMethodCreator newFunction(String pkg, String cxt, boolean inPrototype, String name);

	Iterable<File> files();

}
