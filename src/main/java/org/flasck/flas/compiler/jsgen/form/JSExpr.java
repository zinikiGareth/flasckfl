package org.flasck.flas.compiler.jsgen.form;

import org.flasck.flas.compiler.jsgen.creators.JVMCreationContext;
import org.zinutils.bytecode.mock.IndentWriter;

public interface JSExpr {

	String asVar();
	void write(IndentWriter w);
	void generate(JVMCreationContext jvm);

}
