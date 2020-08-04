package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.zinutils.bytecode.ByteCodeEnvironment;
import org.zinutils.bytecode.mock.IndentWriter;

public interface JSMethodCreator extends JSBlockCreator {
	String jsName();
	JSExpr argument(String name);
	void clear();
	void initContext(PackageName packageName);
	void testComplete();
	void copyContract(JSExpr copyInto, String fld, String arg);
	void write(IndentWriter w, ByteCodeEnvironment bce);
}
