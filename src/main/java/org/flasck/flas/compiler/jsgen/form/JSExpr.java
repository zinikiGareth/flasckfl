package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;

public interface JSExpr {

	String asVar();
	void write(IndentWriter w);

}
