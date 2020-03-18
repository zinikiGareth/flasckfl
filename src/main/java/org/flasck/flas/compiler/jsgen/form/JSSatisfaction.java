package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSSatisfaction implements JSExpr {
	private final String var;

	public JSSatisfaction(String var) {
		this.var = var;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException();
	}

	@Override
	public void write(IndentWriter w) {
		w.print(var);
		w.println(".assertSatisfied(_cxt);");
	}

}
