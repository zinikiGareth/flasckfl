package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.compiler.jsgen.form.JSExpr;

public interface JSMethodCreator extends JSBlockCreator {
	JSExpr argument(String name);
}
