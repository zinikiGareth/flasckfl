package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSRequireContract implements JSExpr {
	private final String var;
	private final String ctr;

	public JSRequireContract(String var, String ctr) {
		this.var = var;
		this.ctr = ctr;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("cannot ask for the var for this since it is void");
	}

	@Override
	public void write(IndentWriter w) {
		w.print("this._contracts.require(_cxt, '");
		w.print(var);
		w.print("', '");
		w.print(ctr);
		w.println("');");
	}

}
