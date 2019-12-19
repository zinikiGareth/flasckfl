package org.flasck.flas.compiler.jsgen.packaging;

import java.io.File;
import java.util.List;

import org.flasck.flas.commonBase.names.FunctionName;
import org.flasck.flas.commonBase.names.SolidName;
import org.flasck.flas.compiler.jsgen.creators.JSClassCreator;
import org.flasck.flas.compiler.jsgen.creators.JSMethodCreator;

public interface JSStorage {

	JSClassCreator newClass(String pkg, String clz);

	void ensurePackageExists(String filePkg, String pkg);

	JSMethodCreator newFunction(String pkg, String cxt, boolean inPrototype, String name);
	void methodList(SolidName name, List<FunctionName> methods);

	Iterable<File> files();


}
