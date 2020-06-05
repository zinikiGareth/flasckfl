package org.flasck.flas.compiler.jsgen.creators;

import org.flasck.flas.compiler.jsgen.form.JSExpr;

public interface JSIfCreator extends JSExpr {
	JSBlockCreator trueCase();
	JSBlockCreator falseCase();
}
