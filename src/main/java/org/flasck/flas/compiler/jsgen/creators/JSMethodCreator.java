package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.zinutils.bytecode.mock.IndentWriter;

public interface JSMethodCreator extends JSBlockCreator {
	String jsName();
	void argumentList();
	JSVar argument(String type, String name);
	JSVar argument(String name);
	void superArg(JSVar a);
	void returnsType(String ty);
	void clear();
	void initContext(PackageName packageName);
	void testComplete();
	void copyContract(JSExpr copyInto, String fld, String arg);
	void write(IndentWriter w);
}
