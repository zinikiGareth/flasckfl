package org.flasck.flas.compiler.jsgen.form;

import org.zinutils.bytecode.mock.IndentWriter;
import org.zinutils.exceptions.NotImplementedException;

public class JSNewDiv implements JSExpr {
	private Integer cnt;

	public JSNewDiv(Integer cnt) {
		this.cnt = cnt;
	}

	@Override
	public String asVar() {
		throw new NotImplementedException("not for use");
	}

	@Override
	public void write(IndentWriter w) {
		w.println("_cxt.newdiv(" + cnt + ")");
	}

}
