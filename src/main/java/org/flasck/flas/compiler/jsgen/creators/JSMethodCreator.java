package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.compiler.jsgen.form.JSExpr;

public interface JSMethodCreator extends JSBlockCreator {
	String jsName();
	JSExpr argument(String name);
	void initContext();
}
