package org.flasck.flas.compiler.jsgen.creators;

import java.util.Set;

import org.flasck.flas.commonBase.names.NameOfThing;
import org.flasck.flas.commonBase.names.PackageName;
import org.flasck.flas.compiler.jsgen.form.JSExpr;
import org.flasck.flas.compiler.jsgen.form.JSVar;
import org.zinutils.bytecode.mock.IndentWriter;

public interface JSMethodCreator extends JSBlockCreator {
	String jsName();
	void noJS();
	void noJVM();
	void argumentList();
	JSVar argument(String type, String name);
	JSVar argument(String name);
	JSVar handlerArg();
	void superArg(JSExpr a);
	void returnsType(String ty);
	void clear();
	void initContext(PackageName packageName);
	void testComplete();
	void copyContract(JSExpr copyInto, String fld, String arg);
	void write(IndentWriter w, Set<NameOfThing> names);
}
