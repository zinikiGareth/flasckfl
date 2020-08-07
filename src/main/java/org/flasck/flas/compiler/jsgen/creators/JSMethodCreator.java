package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.zinutils.bytecode.mock.IndentWriter;

public interface JSMethodCreator extends JSBlockCreator {
	String jsName();
	void argumentList();
	JSExpr argument(String type, String name);
	JSExpr argument(String name);
	void returnsType(String ty);
	void clear();
	void initContext(PackageName packageName);
	void testComplete();
	void copyContract(JSExpr copyInto, String fld, String arg);
	void write(IndentWriter w);
}
